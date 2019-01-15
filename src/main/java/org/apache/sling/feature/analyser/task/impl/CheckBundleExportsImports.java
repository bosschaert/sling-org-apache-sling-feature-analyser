/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.analyser.task.impl;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;
import org.osgi.framework.Version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class CheckBundleExportsImports implements AnalyserTask {
    private static final String GLOBAL_REGION = "global";
    private static final String NO_REGION = " __NO_REGION__ ";

    @Override
    public String getName() {
        return "Bundle Import/Export Check";
    }

    @Override
    public String getId() {
        return "bundle-packages";
    }

    public static final class Report {

        public List<PackageInfo> exportWithoutVersion = new ArrayList<>();

        public List<PackageInfo> exportMatchingSeveral = new ArrayList<>();

        public List<PackageInfo> importWithoutVersion = new ArrayList<>();

        public List<PackageInfo> missingExports = new ArrayList<>();

        public List<PackageInfo> missingExportsWithVersion = new ArrayList<>();

        public List<PackageInfo> missingExportsForOptional = new ArrayList<>();
    }

    private Report getReport(final Map<BundleDescriptor, Report> reports, final BundleDescriptor info) {
        Report report = reports.get(info);
        if ( report == null ) {
            report = new Report();
            reports.put(info, report);
        }
        return report;
    }

    private void checkForVersionOnExportedPackages(final AnalyserTaskContext ctx, final Map<BundleDescriptor, Report> reports) {
        for(final BundleDescriptor info : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            if ( info.getExportedPackages() != null ) {
                for(final PackageInfo i : info.getExportedPackages()) {
                    if ( i.getPackageVersion().compareTo(Version.emptyVersion) == 0 ) {
                        getReport(reports, info).exportWithoutVersion.add(i);
                    }
                }
            }
        }
    }

    private void checkForVersionOnImportingPackages(final AnalyserTaskContext ctx, final Map<BundleDescriptor, Report> reports) {
        for(final BundleDescriptor info : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            if ( info.getImportedPackages() != null ) {
                for(final PackageInfo i : info.getImportedPackages()) {
                    if ( i.getVersion() == null ) {
                        // don't report for javax and org.w3c. packages (TODO)
                        if ( !i.getName().startsWith("javax.")
                             && !i.getName().startsWith("org.w3c.")) {
                            getReport(reports, info).importWithoutVersion.add(i);
                        }
                    }
                }
            }
        }
    }


    @Override
    public void execute(final AnalyserTaskContext ctx) throws IOException {
        // basic checks
        final Map<BundleDescriptor, Report> reports = new HashMap<>();
        checkForVersionOnExportedPackages(ctx, reports);
        checkForVersionOnImportingPackages(ctx, reports);

        final SortedMap<Integer, List<BundleDescriptor>> bundlesMap = new TreeMap<>();
        for(final BundleDescriptor bi : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            List<BundleDescriptor> list = bundlesMap.get(bi.getBundleStartLevel());
            if ( list == null ) {
                list = new ArrayList<>();
                bundlesMap.put(bi.getBundleStartLevel(), list);
            }
            list.add(bi);
        }

        // add all system packages
        final List<BundleDescriptor> exportingBundles = new ArrayList<>();
        if ( ctx.getFrameworkDescriptor() != null ) {
            exportingBundles.add(ctx.getFrameworkDescriptor());
        }

        ApiRegions apiRegions = readAPIRegionsFromFeature(ctx);
        Map<String, Set<String>> bundleToOriginalFeatures = readBundleOrigins(ctx);
        Map<String, Set<String>> featureToOriginalRegions = readRegionOrigins(ctx);

        for(final Map.Entry<Integer, List<BundleDescriptor>> entry : bundlesMap.entrySet()) {
            // first add all exporting bundles
            for(final BundleDescriptor info : entry.getValue()) {
                if ( info.getExportedPackages() != null ) {
                    exportingBundles.add(info);
                }
            }
            // check importing bundles
            for(final BundleDescriptor info : entry.getValue()) {
                if ( info.getImportedPackages() != null ) {
                    for(final PackageInfo pck : info.getImportedPackages() ) {
                        final Map<BundleDescriptor, Set<String>> candidates =
                                getCandidates(exportingBundles, pck,
                                        bundleToOriginalFeatures, featureToOriginalRegions, apiRegions);
                        if ( candidates.isEmpty() ) {
                            if ( pck.isOptional() ) {
                                getReport(reports, info).missingExportsForOptional.add(pck);
                            } else {
                                getReport(reports, info).missingExports.add(pck);
                            }
                        } else {
                            final List<BundleDescriptor> matchingCandidates = new ArrayList<>();

                            for(final Map.Entry<BundleDescriptor, Set<String>> candidate : candidates.entrySet()) {
                                BundleDescriptor bd = candidate.getKey();
                                if (bd.isExportingPackage(pck)) {
                                    Set<String> regions = candidate.getValue();
                                    if (regions.contains(NO_REGION)) {
                                        // If an export is defined outside of a region, it always matches
                                        matchingCandidates.add(bd);
                                        continue;
                                    }
                                    if (regions.contains(GLOBAL_REGION)) {
                                        matchingCandidates.add(bd);
                                        continue;
                                    }

                                    // Find out what regions the importing bundle is in
                                    Set<String> importingBundleRegions =
                                            getBundleRegions(info, bundleToOriginalFeatures, featureToOriginalRegions);

                                    // Only keep the regions that also export the package
                                    importingBundleRegions.retainAll(regions);

                                    if (importingBundleRegions.size() > 0) {
                                        // there is an overlapping region
                                        matchingCandidates.add(bd);
                                    }
                                }
                            }

                            if ( matchingCandidates.isEmpty() ) {
                                if ( pck.isOptional() ) {
                                    getReport(reports, info).missingExportsForOptional.add(pck);
                                } else {
                                    getReport(reports, info).missingExportsWithVersion.add(pck);
                                }
                            } else if ( matchingCandidates.size() > 1 ) {
                                getReport(reports, info).exportMatchingSeveral.add(pck);
                            }
                        }
                    }
                }
            }
        }

        for(final Map.Entry<BundleDescriptor, Report> entry : reports.entrySet()) {
            final String key = "Bundle " + entry.getKey().getArtifact().getId().getArtifactId() + ":" + entry.getKey().getArtifact().getId().getVersion();

            if ( !entry.getValue().importWithoutVersion.isEmpty() ) {
                ctx.reportWarning(key + " is importing package(s) " + getPackageInfo(entry.getValue().importWithoutVersion, false) + " without specifying a version range.");
            }
            if ( !entry.getValue().exportWithoutVersion.isEmpty() ) {
                ctx.reportWarning(key + " is exporting package(s) " + getPackageInfo(entry.getValue().importWithoutVersion, false) + " without a version.");
            }

            if ( !entry.getValue().missingExports.isEmpty() ) {
                ctx.reportError(key + " is importing package(s) " + getPackageInfo(entry.getValue().missingExports, false) + " in start level " +
                        String.valueOf(entry.getKey().getBundleStartLevel()) + " but no bundle is exporting these for that start level.");
            }
            if ( !entry.getValue().missingExportsWithVersion.isEmpty() ) {
                ctx.reportError(key + " is importing package(s) " + getPackageInfo(entry.getValue().missingExportsWithVersion, true) + " in start level " +
                        String.valueOf(entry.getKey().getBundleStartLevel()) + " but no bundle is exporting these for that start level in the required version range.");
            }
        }
    }

    private Set<String> getBundleRegions(BundleDescriptor info, Map<String, Set<String>> bundleToOriginalFeatures,
            Map<String, Set<String>> featureToOriginalRegions) {
        // TODO Auto-generated method stub
        return null;
    }

    private ApiRegions readAPIRegionsFromFeature(final AnalyserTaskContext ctx) {
        Feature feature = ctx.getFeature();
        Extension apiRegionsExtension = feature.getExtensions().getByName("api-regions");
        if (apiRegionsExtension == null)
            return null;

        String apiRegionsJSON = apiRegionsExtension.getJSON();
        if (apiRegionsJSON != null && !apiRegionsJSON.isEmpty()) {
            return ApiRegions.fromJson(apiRegionsJSON);
        } else {
            return null;
        }
    }

    private String getPackageInfo(final List<PackageInfo> pcks, final boolean includeVersion) {
        if ( pcks.size() == 1 ) {
            if (includeVersion) {
                return pcks.get(0).toString();
            } else {
                return pcks.get(0).getName();
            }
        }
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append('[');
        for(final PackageInfo info : pcks) {
            if ( first ) {
                first = false;
            } else {
                sb.append(", ");
            }
            if (includeVersion) {
                sb.append(info.toString());
            } else {
                sb.append(info.getName());
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private Map<BundleDescriptor, Set<String>> getCandidates(
            final List<BundleDescriptor> exportingBundles,
            final PackageInfo pck,
            final Map<String, Set<String>> bundleToOriginalFeatures,
            final Map<String, Set<String>> featureToOriginalRegions,
            final ApiRegions apiRegions) throws IOException {
        final Map<BundleDescriptor, Set<String>> candidates = new HashMap<>();
        for(final BundleDescriptor info : exportingBundles) {
            if ( info.isExportingPackage(pck.getName()) ) {
                Set<String> originFeatures = bundleToOriginalFeatures.get(info.getBundleSymbolicName());

                if (originFeatures != null) {
                    for (String feature : originFeatures) {
                        Set<String> originRegions = featureToOriginalRegions.get(feature);

                        if (originRegions != null) {
                            for (String region : originRegions) {
                                if (!apiRegions.getApis(region).contains(pck.getName())) {
                                    continue;
                                }

                                Set<String> regions = candidates.get(info);
                                if (regions == null) {
                                    regions = new HashSet<>();
                                    candidates.put(info, regions);
                                }
                                regions.add(region);
                            }
                        } else {
                            candidates.put(info, Collections.singleton(NO_REGION));
                        }
                    }
                } else {
                    candidates.put(info, Collections.singleton(NO_REGION));
                }
            }
        }
        return candidates;
    }

    private Map<String, Set<String>> readBundleOrigins(AnalyserTaskContext ctx) throws IOException {
        return readOrigins(ctx, "bundleOrigins.properties");
    }

    private Map<String, Set<String>> readRegionOrigins(AnalyserTaskContext ctx) throws IOException {
        return readOrigins(ctx, "regionOrigins.properties");
    }

    private Map<String, Set<String>> readOrigins(AnalyserTaskContext ctx, String fileName) throws IOException, FileNotFoundException {
        String fileStorage = ctx.getConfiguration().get("fileStorage");
        if (fileStorage == null)
            return Collections.emptyMap();

        Properties p = new Properties();

        File file = new File(fileStorage, fileName);
        if (!file.exists())
            return Collections.emptyMap();

        try (InputStream is = new FileInputStream(file)) {
            p.load(is);
        }

        Map<String, Set<String>> m = new HashMap<>();
        for (String key : p.stringPropertyNames()) {
            String val = p.getProperty(key);
            String[] features = val.split(",");
            m.put(key, new HashSet<>(Arrays.asList(features)));
        }
        return m;
    }
}
