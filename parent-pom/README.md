# Parent POMs

Generates parent POMs for the Horizon projects.


# Warning

The POM files installed by "mvn clean install" are not the pom.xml files at the root of each module!
This is non-standard.

Instead, the built POM.xml in target/classes/pom.xml becomes the final POM file for the module.


# Maintaining

* Add new version properties to the file:

    SNIPPETS/versions.snippet


* Add new dependencies to the file:

    SNIPPETS/dependencies.snippet


# Why this approach?

* Common dependency versions across the entire project.
* May need to use external parent POMs (such as the spring boot parent).
* Maven ONLY supports properties for dependency versions when the properties are directly defined in the project's
  POM files (including the parent).
  * BOM properties are ignored on import of the BOM
  * Other means of setting properties (e.g. properties-maven-plugin) are too late for use as dependency versions 
* Single source of versions is maintained (the `SNIPPETS/versions.snippet` file)
* Properties with those versions are available to the project for other uses (such as generating Karaf feature files)
