# Horizon Stream

**Horizon Stream** is a new distribution of OpenNMS, inspired from the existing platform

See [Getting Started on the Wiki](https://github.com/OpenNMS/lokahi/wiki/Getting-Started) for development instructions.

# Rapid Development Profile

In order to facilitate more rapid development,
the Maven profile named rapid-build
disables Intergation Tests (Failsafe) and
the Karaf Maven Plugin's validation of features.

* It can be enabled in the following ways:
  * In standard Maven ways:
    * Manually add `-P rapid-build` to individual `mvn` commands
    * Add the `rapid-build` profile to the user's `settings.xml` file in the `<activeProfiles>` section
  * Create a file at `${user.home}/.OPENNMS-RAPID-BUILD`
