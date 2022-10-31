[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-ide-tooling/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-ide-tooling/job/master/)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-ide-tooling&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-ide-tooling) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling IDE Tooling

This module is part of the [Apache Sling](https://sling.apache.org) project.

For using the IDE tooling, please see the [Sling IDE Tooling](https://sling.apache.org/documentation/development/ide-tooling.html)
documentation page.

## Repository structure

The modules are split into distinct sub-trees

* shared
* cli
* eclipse

to ensure that the reusable code is available for usage in other IDEs or
environments.

The modules placed under `shared/modules` should bring as few external dependencies as
possible, and must not depend on IDE-specific APIs, such as Eclipse or OSGi. However,
all modules come with OSGi bundle headers and provide even some OSGi DS components.

The modules placed under `eclipse` may depend on any Eclipse-specific APIs.

To make the shared modules consumable by the Maven + Tycho toolchain, a separate
`shared/p2` sub-tree contains a Maven + Tycho build which creates a p2 update
site. That update site in turn is consumed by the `eclipse` build.

## Building the Sling IDE Tooling for Eclipse

This howto assumes that you are running Eclipse Oxygen or later with the Plug-In 
Development Environment and Maven features installed. You should have
previously built the projects using

    ./build.sh

to ensure that Maven artifacts which are not available on p2 update sites are
included in the workspace.

To start with, import all the projects in Eclipse as Maven projects. Eclipse
might prompt you to install an additional m2eclipse configurator for PDE
projects, as it's needed for bridging between Maven and PDE projects.

After the projects are imported, you need to set your target environment to
ensure that all dependencies are met and that you are working against the
project's declared baseline. To do that, open the following file in Eclipse

    target-definition/org.apache.sling.ide.target-definition-dev.target

In the target editor which appears, click 'Set as Target Platform'. Once
the target platform is set up, you can create a new launch configuration.

  NOTE: if you don't see a target editor, but an XML editor, try right-clicking
  on the file and choosing File -> Open With -> Target Editor. If you don't
  see that option, you don't have PDE installed.

Now you can use the 'Sling IDE Tooling' launch configuration which is present 
in the org.apache.sling.ide.target-definition project to launch a local instance
of Eclipse with Sling IDE Tooling plug-ins picked up from the local workspace.

Due to the unclear future of https://github.com/tesla/m2eclipse-tycho (compare with https://github.com/eclipse-m2e/m2e-core/issues/605)
for the time being the pom.xml configuration is duplicated in the project settings (which is checked in)
