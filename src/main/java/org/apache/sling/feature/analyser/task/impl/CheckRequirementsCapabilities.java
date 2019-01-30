/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.analyser.task.impl;

import org.apache.felix.utils.resource.CapabilitySet;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.Descriptor;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Requirement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class CheckRequirementsCapabilities implements AnalyserTask {
    private final String format = "Artifact %s requires %s in start level %d but %s";

    @Override
    public String getId() {
        return "requirements-capabilities";
    }

    @Override
    public String getName() {
        return "Requirements Capabilities check";
    }

    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {
        final SortedMap<Integer, List<Descriptor>> artifactsMap = new TreeMap<>();
        for(final BundleDescriptor bi : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            List<Descriptor> list = artifactsMap.get(bi.getBundleStartLevel());
            if ( list == null ) {
                list = new ArrayList<>();
                artifactsMap.put(bi.getBundleStartLevel(), list);
            }
            list.add(bi);
        }

        if (!ctx.getFeatureDescriptor().getArtifactDescriptors().isEmpty()) {
            artifactsMap.put(
                    (artifactsMap.isEmpty() ? 0 : artifactsMap.lastKey()) + 1,
                    new ArrayList<>(ctx.getFeatureDescriptor().getArtifactDescriptors())
                    );
        }

        // add Feature artifact
        artifactsMap.put(999 /* TODO */,
                Collections.singletonList(ctx.getFeatureDescriptor()));

        // add system artifact
        final List<Descriptor> artifacts = new ArrayList<>();
        if ( ctx.getFrameworkDescriptor() != null ) {
            artifacts.add(ctx.getFrameworkDescriptor());
        }

        for(final Map.Entry<Integer, List<Descriptor>> entry : artifactsMap.entrySet()) {
            // first add all providing artifacts
            for (final Descriptor info : entry.getValue()) {
                if (info.getCapabilities() != null) {
                    artifacts.add(info);
                }
            }
            // check requiring artifacts
            for (final Descriptor info : entry.getValue()) {
                if (info.getRequirements() != null)
                {
                    for (Requirement requirement : info.getRequirements()) {
                        if (!BundleRevision.PACKAGE_NAMESPACE.equals(requirement.getNamespace()))
                        {
                            List<Descriptor> candidates = getCandidates(artifacts, requirement);

                            if (candidates.isEmpty())
                            {
                                if ("osgi.service".equals(requirement.getNamespace()))
                                {
                                    // osgi.service is special - we don't provide errors or warnings in this case
                                    continue;
                                }
                                if (!RequirementImpl.isOptional(requirement))
                                {
                                    ctx.reportError(String.format(format, info.getName(), requirement.toString(), entry.getKey(), "no artifact is providing a matching capability in this start level."));
                                }
                                else
                                {
                                    ctx.reportWarning(String.format(format, info.getName(), requirement.toString(), entry.getKey(), "while the requirement is optional no artifact is providing a matching capability in this start level."));
                                }
                            }
                            else if (candidates.size() > 1)
                            {
                                ctx.reportWarning(String.format(format, info.getName(), requirement.toString(), entry.getKey(), "there is more than one matching capability in this start level."));
                            }
                        }
                    }
                }
            }
        }
    }

    private List<Descriptor> getCandidates(List<Descriptor> artifactDescriptors, Requirement requirement) {
        return artifactDescriptors.stream()
                .filter(artifactDescriptor -> artifactDescriptor.getCapabilities() != null)
                .filter(artifactDescriptor -> artifactDescriptor.getCapabilities().stream().anyMatch(capability -> CapabilitySet.matches(capability, requirement)))
                .collect(Collectors.toList());
    }
}
