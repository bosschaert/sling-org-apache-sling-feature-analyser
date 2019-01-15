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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.impl.BundleDescriptorImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class CheckBundleExportsImportsTest {
    private static File resourceRoot;

    @BeforeClass
    public static void setupClass() {
        resourceRoot =
                new File(CheckBundleExportsImportsTest.class.
                        getResource("/test-framework.jar").getFile()).getParentFile();
    }

    @Test
    public void testId() {
        assertEquals("bundle-packages",
                new CheckBundleExportsImports().getId());
    }

    @Test
    public void testImportExportNoRegionsAllOk() throws IOException {
        CheckBundleExportsImports t = new CheckBundleExportsImports();

        Feature f = new Feature(ArtifactId.fromMvnId("f:f:1"));
        FeatureDescriptor fd = new FeatureDescriptor() {
            @Override
            public Feature getFeature() {
                return f;
            }
        };

        fdAddBundle(fd, "g:b1:1", "test-bundle1.jar");
        fdAddBundle(fd, "g:b2:1", "test-bundle2.jar");

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(f);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);
        t.execute(ctx);

        Mockito.verify(ctx, Mockito.never()).reportError(Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportWarning(Mockito.anyString());
    }

    @Test
    public void testImportExportNoRegionsMissing() throws IOException {
        CheckBundleExportsImports t = new CheckBundleExportsImports();

        Feature f = new Feature(ArtifactId.fromMvnId("f:f:1"));
        FeatureDescriptor fd = new FeatureDescriptor() {
            @Override
            public Feature getFeature() {
                return f;
            }
        };

        fdAddBundle(fd, "g:b1:1", "test-bundle1.jar");
        fdAddBundle(fd, "g:b3:1", "test-bundle3.jar");

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(f);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);
        t.execute(ctx);

        Mockito.verify(ctx).reportError(Mockito.contains("org.foo.e"));
        Mockito.verify(ctx, Mockito.times(1)).reportError(Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportWarning(Mockito.anyString());
    }

    @Test
    /*
     * Bundle 2 imports org.foo.b from bundle 1, but bundle 1 exports it in a different
     * region, bundle 2 is in no region.
     */
    public void testImportExportWithRegionsMissing() throws Exception {
        String exJson = "[{\"name\": \"something\", \"exports\": [\"org.foo.b\"]}]";

        CheckBundleExportsImports t = new CheckBundleExportsImports();

        Feature f = new Feature(ArtifactId.fromMvnId("f:f:1"));
        Extension ex = new Extension(ExtensionType.JSON, "api-regions", false);
        ex.setJSON(exJson);
        f.getExtensions().add(ex);

        FeatureDescriptor fd = new FeatureDescriptor() {
            @Override
            public Feature getFeature() {
                return f;
            }
        };

        fdAddBundle(fd, "g:b1:1", "test-bundle1.jar");
        fdAddBundle(fd, "g:b2:1", "test-bundle2.jar");

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(f);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);
        Mockito.when(ctx.getConfiguration()).thenReturn(
                Collections.singletonMap("fileStorage",
                        resourceRoot + "/origins/testImportExportWithRegionsMissing"));
        t.execute(ctx);

        Mockito.verify(ctx).reportError(Mockito.contains("org.foo.b"));
        Mockito.verify(ctx, Mockito.times(1)).reportError(Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportWarning(Mockito.anyString());
    }

    @Test
    /*
     * Bundle 2 imports org.foo.b from bundle 1, but bundle 1 exports it in a different
     * region, bundle 1 is in something region, and bundle 2 is in somethingelse region.
     */
    public void testImportExportWithRegionMismatch() throws Exception {
        String exJson = "[{\"name\": \"something\", \"exports\": [\"org.foo.b\"]}]";

        CheckBundleExportsImports t = new CheckBundleExportsImports();

        Feature f = new Feature(ArtifactId.fromMvnId("f:f:1"));
        Extension ex = new Extension(ExtensionType.JSON, "api-regions", false);
        ex.setJSON(exJson);
        f.getExtensions().add(ex);

        FeatureDescriptor fd = new FeatureDescriptor() {
            @Override
            public Feature getFeature() {
                return f;
            }
        };

        fdAddBundle(fd, "g:b1:1", "test-bundle1.jar");
        fdAddBundle(fd, "g:b2:1", "test-bundle2.jar");

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(f);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);
        Mockito.when(ctx.getConfiguration()).thenReturn(
                Collections.singletonMap("fileStorage",
                        resourceRoot + "/origins/testImportExportWithRegionMismatch"));
        t.execute(ctx);

        Mockito.verify(ctx).reportError(Mockito.contains("org.foo.b"));
        Mockito.verify(ctx).reportError(Mockito.contains("something"));
        Mockito.verify(ctx).reportError(Mockito.contains("somethingelse"));
        Mockito.verify(ctx, Mockito.times(1)).reportError(Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportWarning(Mockito.anyString());
    }


    /*

    @Test
    public void testImportExportWithRegionsDifferent() throws Exception {
        String exJson = "[{\"name\": \"something\", \"exports\": [\"org.foo.b\"]}]";

        CheckBundleExportsImports t = new CheckBundleExportsImports();

        Feature f = new Feature(ArtifactId.fromMvnId("f:f:1"));
        Extension ex = new Extension(ExtensionType.JSON, "api-regions", false);
        ex.setJSON(exJson);
        f.getExtensions().add(ex);

        FeatureDescriptor fd = new FeatureDescriptor() {
            @Override
            public Feature getFeature() {
                return f;
            }
        };

        fdAddBundle(fd, "g:b1:1", "test-bundle1.jar");
        fdAddBundle(fd, "g:b2:1", "test-bundle2.jar");

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(f);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);
        Mockito.when(ctx.getConfiguration()).thenReturn(
                Collections.singletonMap("fileStorage",
                        resourceRoot + "/origins/testImportExportWithRegionsMissing"));
        t.execute(ctx);

        Mockito.verify(ctx).reportError(Mockito.contains("org.foo.b"));
        Mockito.verify(ctx, Mockito.times(1)).reportError(Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportWarning(Mockito.anyString());
    }

    */
    private void fdAddBundle(FeatureDescriptor fd, String id, String file) throws IOException {
        BundleDescriptor bd1 = new BundleDescriptorImpl(
                new Artifact(ArtifactId.fromMvnId(id)), new File(resourceRoot, file), 0);
        fd.getBundleDescriptors().add(bd1);
    }
}
