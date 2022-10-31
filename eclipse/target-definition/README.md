# Apache Sling IDE Tools Target

The target definition for Eclipse PDE/Tycho against which to build the Eclipse plugins.

The target file references both Eclipse Plugins/Features from P2 repositories (standard Eclipse dependencies) as well as [Maven artifacts](<https://xn--lubisoft-0za.gmbh/en/articles/using-maven-artifacts-in-pde-rcp-and-tycho-builds/>) for custom OSGi bundles.

3rd party dependencies are referenced from [Eclipse Orbit](https://www.eclipse.org/orbit/) (to reuse bundle versions also used by other Eclipse plugins).
