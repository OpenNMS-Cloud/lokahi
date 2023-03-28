#!/bin/bash

USER=admin

kubectl exec -it deployment.apps/opennms-minion -- /bin/bash
