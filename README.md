# jwiki
[![Build Status](https://www.travis-ci.com/benjaminkomen/jwiki.svg?branch=master)](https://www.travis-ci.com/benjaminkomen/jwiki)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=benjaminkomen%3Ajwiki&metric=alert_status)](https://sonarcloud.io/dashboard?id=benjaminkomen%3Ajwiki)
[![License: GPL v3](https://upload.wikimedia.org/wikipedia/commons/8/86/GPL_v3_Blue_Badge.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)

This is a fork of [jwiki by fastily](https://github.com/fastily/jwiki). This fork attempts to be compatible with
 MediaWiki 1.19 to support Wikia wikis.

Try out the [examples](https://github.com/benjaminkomen/jwiki/wiki/Examples).

## Download
 jwiki is [on GitHub Packages](https://github.com/benjaminkomen/jwiki/packages?package_type=Maven).

#### Maven
```xml
<dependency>
  <groupId>benjaminkomen</groupId>
  <artifactId>jwiki</artifactId>
  <version>2.2.0</version>
  <type>pom</type>
</dependency>
```

#### Gradle
```groovy
compile 'benjaminkomen:jwiki:2.2.0'
```

## Build
Build and publish jwiki on your local machine with
```bash
./gradlew build publishToMavenLocal
```

Publishing to GitHub Packages is done with:
```bash
./gradlew publish
```

## Resources
* [Examples](https://github.com/benjaminkomen/jwiki/wiki/Examples)
<!-- * [Javadocs](https://fastily.github.io/jwiki/docs/jwiki/) -->

## Feedback
Please use [issues](https://github.com/benjaminkomen/jwiki/issues) for bug reports and/or feature requests.
