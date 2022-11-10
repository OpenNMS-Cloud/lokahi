# Platform

Currently in proof-of-concept phase

## General approach so far

* Start over with a new module tree
* Use a BOM for managing dependencies
* Revised naming pattern for modules
* Only keep units that work with limited changes
* Delete all ITs and Spring wiring
* Rewrite wiring in Blueprint
* Combine APIs into domains instead of function i.e. (events/api vs opennms-dao-api)
* Rename packages as necessary
* Create new Karaf feature files, pull from existing as needed
* Use the `karaf-maven-plugin` to verify the Karaf features

## Must do

* Review configuration handling - PostgreSQL URL, K8S, Secrets
* Split Minion & Core
    * Figure out best way to setup the features - minimize the code loaded on Minion
* Model definition, validation & lifecycle
    * State vs configuration vs limits & tuning
    * Common model (OnmsNode) vs domain specific (OnmsAlarm)
    * Bundle organization & DAO services
* Authentication & permission model
    * Use standalone auth server, and JWTs
* Managing secrets
* Pull in CM engine
* Observability: OpenMetrics
* docker: arm support & signing/content trust


## Nice to do

* Map our custom criteria classes to JPA?
* Move to JDK 17

## Testing on Karaf

```
feature:repo-add mvn:org.opennms.horizon.assemblies/features/0.1.0-SNAPSHOT/xml/features
feature:install aries-blueprint horizon-icmp
opennms:ping -l cloud 127.0.0.1
```

```
feature:repo-add mvn:org.opennms.horizon.assemblies/features/0.1.0-SNAPSHOT/xml/features
feature:install aries-blueprint horizon-alarms horizon-icmp horizon-provision
```

```
curl -v -u admin:admin http://localhost:8181/alarms/list```
```

```
config:edit org.opennms.netmgt.distributed.datasource
property-set datasource.adminUrl jdbc:postgresql://localhost:5432/template1
property-set datasource.adminUsername postgres
config:update
```

## Building

    # Simple build - no Integration Tests
    $ mvn clean install

    # Full build, including docker image creation and Integration Tests
    $ mvn -Prun-it clean install

    # Full build, the old way
    $ mvn -DskipITs=false clean install docker:build


## Keycloak

Covered below:
* Configuration
* Server Quick Start

### Configuration

**File `etc/keycloak-direct-access.json`**

```
{
  "auth-server-url":"http://localhost:9000",
  "credentials": {
    "secret":"passw0rd"
  },
  "realm":"opennms",
  "resource":"ssh-jmx-admin-client",
  "ssl-required":"none"
}
```

**File `etc/org.opennms.core.rest`**

```
http.realm=keycloak
```


### Server Quick Start Guide

* Download keycloak from https://github.com/keycloak/keycloak/releases/download/17.0.0/keycloak-17.0.0.zip

      $ curl -L -O https://github.com/keycloak/keycloak/releases/download/17.0.0/keycloak-17.0.0.zip

* Install

      $ unzip keycloak-17.0.0.zip

* Configure

**File `conf/keycloak.conf`**

      http-port=9000
      https-port=9443

* Start the server

      $ cd keycloak-17.0.0
      $ bin/kc.sh start-dev

* Open the Keycloak console

      $ open http://localhost:9000

* Follow the on-screen instructions to configure the admin password and open the Admin Console

* Add a Realm (Hover over the "Master" realm name in the top-left and click "Add Realm")

  * Name the realm `opennms`

* Add a Client

  * Click "Clients" on the left-hand navigation panel
  * Click the "Create" button on the top right-hand side of the "Clients" panel
  * Use client ID `ssh-jmx-admin-client`
  * On the "ssh-jmx-admin-client" page, click the "Keys" tab
  * Add a key
    * Click "Generate new keys and certificate"
    * Enter key and store passwords (recommend `passw0rd` for dev)
    * Click "Generate and Download"
    * Note this will download a `keystore.jks` file. Save this file for possible later use.
  * Add the `ssh` role
    * Click "Roles" on the left-hand Navigation panel
    * Click "Add Role" button in the top right-hand side of the "Roles" panel.
    * Enter `ssh` for the "Role Name"
    * Enter a description, such as `User can access the SSH server`
  * Add the user `admin`
    * NOTE: this user is separate from the Administrator account, also named `admin`, used to login to keycloak
    * Click "Users" on the left-hand Navigation panel
    * Click "Add User" on the top right-hand side of the "Users" panel
    * Enter `admin` for the Username
    * Click the "Save" button
    * Set the password
      * On the "Users > admin" panel which opens, click the "Credentials" tab
      * Enter a password (recommend `admin`)
      * Uncheck the Temporary setting
      * Click "Set Password"
    * Add role mappings
      * Click the "Role Mappings" tab
      * Click `ssh` under "Available Roles"
      * Click the "Add Selected >" button
  * Add the user `user001`
    * Click "Users" on the left-hand Navigation panel
    * Click "Add User" on the top right-hand side of the "Users" panel
    * Enter `user001` for the Username
    * Click the "Save" button
    * Set the password
      * On the "Users > admin" panel which opens, click the "Credentials" tab
      * Enter a password (recommend `passw0rd`)
      * Uncheck the Temporary setting
      * Click "Set Password"

## CONFIGURATION -- ENVIRONMENT VARIABLES

| ENV VAR | Config File | Config Property Name | Default |
| ------- | ----------- | -------------------- | ------- |
| ``TASK_SET_GRPC_HOST`` | ``org.opennms.core.taskset.client`` | ``host`` | ``localhost`` |
| ``TASK_SET_GRPC_PORT`` | ``org.opennms.core.taskset.client`` | ``port`` | ``8991`` |
| ``TASK_SET_GRPC_TLS_ENABLED`` | ``org.opennms.core.taskset.client`` | ``tlsEnabled`` | ``false`` |
| ``TASK_SET_GRPC_MAX_MESSAGE_SIZE`` | ``org.opennms.core.taskset.client`` | ``maxMessageSize`` | ``10485760`` |
| ``RPC_REQUEST_GRPC_HOST`` | ``org.opennms.core.rpc.request.client`` | ``host`` | ``localhost`` |
| ``RPC_REQUEST_GRPC_PORT`` | ``org.opennms.core.rpc.request.client`` | ``port`` | ``8991`` |
| ``RPC_REQUEST_GRPC_TLS_ENABLED`` | ``org.opennms.core.rpc.request.client`` | ``tlsEnabled`` | ``false`` |
| ``RPC_REQUEST_GRPC_MAX_MESSAGE_SIZE`` | ``org.opennms.core.rpc.request.client`` | ``maxMessageSize`` | ``10485760`` |
