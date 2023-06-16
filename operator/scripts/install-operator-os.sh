#!/usr/bin/env bash
#use this script to install a basic version of the opennms operator locally

echo
echo _______________Building Docker Image_______________
echo
make local-docker
if [ $? -ne 0 ]; then exit; fi

echo
echo _________________Push Docker Image_________________
echo

eval $(crc oc-env)
REGISTRY="$(oc get route/default-route -n openshift-image-registry -o=jsonpath='{.spec.host}')/openshift"
docker login -u kubeadmin -p $(oc whoami -t) $REGISTRY
docker tag opennms/operator:local-build $REGISTRY/opennms-operator:local
docker push $REGISTRY/opennms-operator:local

echo
echo ________________Installing Operator________________
echo
helm upgrade -i operator-local ../charts/lokahi-operator -f scripts/openshift-operator-values.yaml --namespace opennms --create-namespace
oc adm policy add-role-to-user system:image-pullers system:serviceaccount:opennms-operator -n opennms
if [ $? -ne 0 ]; then exit; fi
