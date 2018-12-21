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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**
 * A "Sieve" data structure to check exported packages
 */
final class ApiRegions {

    private static final String NAME_KEY = "name";

    private static final String EXPORTS_KEY = "exports";

    static ApiRegions fromJson(String jsonRepresentation) {
        ApiRegions apiRegions = new ApiRegions();

        // pointers
        Event event;
        String region = null;
        Collection<String> apis = null;

        JsonParser parser = Json.createParser(new StringReader(jsonRepresentation));
        while (parser.hasNext()) {
            event = parser.next();
            if (Event.KEY_NAME == event) {
                switch (parser.getString()) {
                    case NAME_KEY:
                        parser.next();
                        region = parser.getString();
                        break;

                    case EXPORTS_KEY:
                        apis = new LinkedList<>();

                        // start array
                        parser.next();

                        while (parser.hasNext() && Event.VALUE_STRING == parser.next()) {
                            String api = parser.getString();
                            // skip comments
                            if ('#' != api.charAt(0)) {
                                apis.add(api);
                            }
                        }

                        break;

                    default:
                        break;
                }
            } else if (Event.END_OBJECT == event) {
                if (region != null && apis != null) {
                    apiRegions.add(region, apis);
                }

                region = null;
                apis = null;
            }
        }

        return apiRegions;
    }

    // Use Linked Hash Map to keep the order of the regions as specified in the JSON
    private final Map<String, Set<String>> apis = new LinkedHashMap<>();

    ApiRegions() {
        // it should not be directly instantiated outside this package
    }

    void add(String region, Collection<String> exportedApis) {
        apis.computeIfAbsent(region, k -> new TreeSet<>()).addAll(exportedApis);
    }

    /**
     * Return the regions in the order they were listed.
     * @return The regions.
     */
    List<String> getRegions() {
        // As the regions are stored in a LinkedHashMap the order is preserved.
        // So we can return the keys.
        return new ArrayList<>(apis.keySet());
    }

    Set<String> getApis(String region) {
        return apis.computeIfAbsent(region, k -> Collections.emptySet());
    }

    void remove(String packageName) {
        apis.values().forEach(apis -> apis.remove(packageName));
    }

    boolean isEmpty() {
        for (Set<String> packages : apis.values()) {
            if (!packages.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return apis.toString().replace(',', '\n');
    }
}
