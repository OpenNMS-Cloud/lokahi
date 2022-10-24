#!/bin/bash

USER=admin

kubectl exec -it deployment.apps/opennms-core -- /bin/bash
