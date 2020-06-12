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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Artifacts;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CheckFeatureEquivalenceTest {
    @Test
    public void testAssertEmptyArtifactsSame() {
        Artifacts a1 = new Artifacts();
        Artifacts a2 = new Artifacts();

        assertNull(CheckFeatureEquivalence.assertArtifactsSame(a1, a2, true));
        assertNull(CheckFeatureEquivalence.assertArtifactsSame(a1, a2, false));
    }

    @Test
    public void testAssertDiffSizeArtifactsNotSame() {
        Artifacts a1 = new Artifacts();
        Artifacts a2 = new Artifacts();
        a2.add(new Artifact(ArtifactId.fromMvnId("a:b:1")));

        assertNotNull(CheckFeatureEquivalence.assertArtifactsSame(a1, a2, true));
        assertNotNull(CheckFeatureEquivalence.assertArtifactsSame(a1, a2, false));
    }

    @Test
    public void testAssertDiffSizeArtifactsNotSame2() {
        Artifacts a1 = new Artifacts();
        a1.add(new Artifact(ArtifactId.fromMvnId("a:b:1")));
        Artifacts a2 = new Artifacts();
        a2.add(new Artifact(ArtifactId.fromMvnId("a:b:2")));

        assertNotNull(CheckFeatureEquivalence.assertArtifactsSame(a1, a2, true));
        assertNotNull(CheckFeatureEquivalence.assertArtifactsSame(a1, a2, false));
    }

    @Test
    public void testAssertArtifactsSame() {
        Artifacts a1 = new Artifacts();
        a1.add(new Artifact(ArtifactId.fromMvnId("a:b:1")));
        a1.add(new Artifact(ArtifactId.fromMvnId("a:b:2")));
        Artifacts a2 = new Artifacts();
        a2.add(new Artifact(ArtifactId.fromMvnId("a:b:2")));
        a2.add(new Artifact(ArtifactId.fromMvnId("a:b:1")));

        assertNull(CheckFeatureEquivalence.assertArtifactsSame(a1, a2, true));
        assertNull(CheckFeatureEquivalence.assertArtifactsSame(a1, a2, false));
    }

    @Test
    public void testAssertArtifactsSameDifferentMetadata() {
        Artifact art1 = new Artifact(ArtifactId.fromMvnId("a:b:1"));
        art1.getMetadata().put("foo", "bar");
        Artifacts a1 = new Artifacts();
        a1.add(art1);

        Artifact art2 = new Artifact(ArtifactId.fromMvnId("a:b:1"));
        Artifacts a2 = new Artifacts();
        a2.add(art2);

        assertNotNull(CheckFeatureEquivalence.assertArtifactsSame(a1, a2, true));
        assertNull(CheckFeatureEquivalence.assertArtifactsSame(a1, a2, false));
    }
}