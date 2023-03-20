#!/usr/bin/env bash
#use this script to install a basic version of OpenNMS Horizon Stream locally

echo
echo ____________Installing Local Instance______________
echo
kubectl apply -f scripts/local-os-onms-instance.yaml
if [ $? -ne 0 ]; then exit; fi

