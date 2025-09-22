# Apache Sling IDE Tools Target

The target definition for Eclipse PDE/Tycho against which to build the Eclipse plugins.

The target file references Eclipse Plugins/Features from P2 repositories (standard Eclipse dependencies). The final target platform is enhanced by configuring `pomDependencies=consider` for the `target-platform-configuration` in the reactor pom.xml

3rd party dependencies are referenced from [Eclipse Orbit](https://www.eclipse.org/orbit/) (to reuse bundle versions also used by other Eclipse plugins).
