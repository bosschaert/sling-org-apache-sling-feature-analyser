[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=sling-org-apache-sling-feature-analyser-1.8)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-feature-analyser-1.8) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.feature.analyser/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.feature.analyser%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.feature.analyser.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.feature.analyser) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/feature.md)

# Feature Model Analyser

The Analyser can analyse features for completeness and correctness. The analyser is pluggable and can also perform other checks.

The analyser can be run from the commandline by running the following main class:

```
java org.apache.sling.feature.analyser.main.Main
```

# Feature Model Analyser as a Maven Plugin

The Analyser can also be run as part of a maven build via the `slingfeature-maven-plugin`: https://github.com/apache/sling-slingfeature-maven-plugin

The following analysers are defined:

* `bundle-packages`: Checks bundle import/export package statements for consistency and completeness. If API Regions are used this analyser includes this 
information as part of the check, to ensure that bundles don't import packages of which they have no visibility because of API Regions restrictions.

* `bundle-content`: Gives a warning if a bundle container initial content specified with `Sling-Initial-Content`.

* `bundle-resources`: Gives a warning if a bundle contains resources specified with `Sling-Bundle-Resources`.

* `requirements-capabilities`: Checks bundle requirements/capabilities for consistency and completeness.


* `apis-jar`: validates that the `sourceId` property of a bundle, if defined, is a comma-separated value list of artifact ids.

Additional analysers in relation to Feature Model API Regions can be found here: https://github.com/apache/sling-org-apache-sling-feature-extension-apiregions

For further documentation see: https://github.com/apache/sling-org-apache-sling-feature/blob/master/readme.md
