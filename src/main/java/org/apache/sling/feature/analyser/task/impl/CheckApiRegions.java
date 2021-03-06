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

import java.util.Collection;
import java.util.Formatter;
import java.util.Set;

import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;

public class CheckApiRegions extends AbstractApiRegionsAnalyserTask {

    @Override
    public String getId() {
        return API_REGIONS_KEY;
    }

    @Override
    public String getName() {
        return "Api Regions analyser task";
    }

    @Override
    protected void execute(ApiRegions apiRegions, AnalyserTaskContext ctx) throws Exception {
        // for each bundle, get the Export-Package and process the packages

        FeatureDescriptor featureDescriptor = ctx.getFeatureDescriptor();
        for (BundleDescriptor bundleDescriptor : featureDescriptor.getBundleDescriptors()) {
            for (PackageInfo packageInfo : bundleDescriptor.getExportedPackages()) {
                String exportedPackage = packageInfo.getName();
                // use the Sieve technique: remove bundle exported packages from the api-regions
                apiRegions.remove(exportedPackage);
            }
        }

        // final evaluation: if the Sieve is not empty, not all declared packages are exported by bundles of the same feature
        if (!apiRegions.isEmpty()) {
            // track a single error for each region
            for (String region : apiRegions.getRegions()) {
                Set<String> apis = apiRegions.getApis(region);
                if (!apis.isEmpty()) {
                    Formatter formatter = new Formatter();
                    formatter.format("Region '%s' defined in feature '%s' declares %s package%s which %s not exported by any bundle:%n",
                                     region,
                                     ctx.getFeature().getId(),
                                     apis.size(),
                                     getExtension(apis, "", "s"),
                                     getExtension(apis, "is", "are"));
                    apis.forEach(api -> formatter.format(" * %s%n", api));

                    ctx.reportError(formatter.toString());

                    formatter.close();
                }
            }
        }
    }

    // utility methods

    private static <T> String getExtension(Collection<T> collection, String singular, String plural) {
        return collection.size() > 1 ? plural : singular;
    }

}
