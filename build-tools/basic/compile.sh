#!/bin/sh
########################################################################################################################
##
## FULL PROJECT BUILD INSTRUCTIONS
##
## !!! NOTE !!!
##  KEEP THE INSTRUCTIONS IN THIS SCRIPT, AND ANY OTHER SCRIPTS IT USES, SIMPLE.
##  THE FOLLOWING BELONGS HERE:
##    - TOP-LEVEL PROJECT BUILD ORDER
##    - NAVIGATING THE DIRECTORY HIERARCHY TO TOP-LEVEL PROJECTS
##    - SIMPLE TOP-LEVEL PROJECT BUILD INSTRUCTIONS
##      (e.g. "mvn clean install" or "./build.sh")
##
## GOALS:
##	- Full build of the the mono-repo, including all Top Level projects, IS a GOAL.
##	- Initail build of the mono-repo on checkout, once base setup and pre-requisites are met IS a GOAL.
##
## NON-GOALS:
##	- Incremental builds of the full mono-repo are NOT a GOAL.
##	- Detecting missing pre-requisites and setup is NOT a GOAL.
##
########################################################################################################################

set -e

mvn clean install -f parent-pom
mvn clean install -f shared-lib

mvn clean install -f alarm
mvn clean install -f minion-gateway
mvn clean install -f minion-gateway-grpc-proxy
mvn clean install -f minion
mvn clean install -f rest-server
mvn clean install -f inventory
mvn clean install -f notifications
mvn clean install -f metrics-processor
mvn clean install -f events
mvn clean install -f datachoices
