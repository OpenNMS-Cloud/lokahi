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
## Please keep unit tests enabled here; feel free to copy this script and keep local changes as you see fit - just
##  be careful not to commit them.
##
########################################################################################################################

set -e

mvn clean install -P rapid-build -f parent-pom
mvn clean install -P rapid-build -f shared-lib

mvn clean install -P rapid-build -f alert
mvn clean install -P rapid-build -f minion-gateway
mvn clean install -P rapid-build -f minion-certificate-manager
mvn clean install -P rapid-build -f minion-certificate-verifier
mvn clean install -P rapid-build -f minion
mvn clean install -P rapid-build -f rest-server
mvn clean install -P rapid-build -f inventory
mvn clean install -P rapid-build -f notifications
mvn clean install -P rapid-build -f metrics-processor
mvn clean install -P rapid-build -f events
mvn clean install -P rapid-build -f datachoices
