#!/usr/bin/env bash
#use this script to install a basic version of OpenNMS Horizon Stream locally

set -e

LOCAL_DOCKER_CONFIG_JSON="${HOME}/.docker/config.json"

#### ENV VARS
################################

# For local, we can setup localhost as the default on port 8080 or something.
# Like Skaffold and Tilt. 
# This determines whether or not to import custom images or not.
HELP='Need to pass "local" parameter to script'
if [[ -z "$2" || -z "$1" ]]; then
  echo "Need to add custom DNS for the second parameter, domain to use."
  echo "$HELP"
  exit 1
fi

CONTEXT=$1
DOMAIN=$2
IMAGE_TAG=${3:-local}
IMAGE_PREFIX=${4:-opennms}
KIND_CLUSTER_NAME=kind-test
NAMESPACE=hs-instance
TIMEOUT=${TIMEOUT:-5m0s}

#### FUNCTION DEF
################################

function die() {
  echo "Error: $1"
  exit 1
}

create_cluster() {
  echo
  echo ______________Creating Kind Cluster________________
  echo
  kind create cluster --name $KIND_CLUSTER_NAME --config=./install-local-kind-config.yaml
  kubectl config use-context "kind-$KIND_CLUSTER_NAME"
  kubectl config get-contexts
}

cluster_ready_check () {

  # This is the last pod to run, if ready, then give back the terminal session.
  sleep 60 # Need to wait until the pod is created or else nothing comes back. Messes with the conditional.
  while [[ $(kubectl get pods -n $NAMESPACE -l=app.kubernetes.io/component="controller-$NAMESPACE" -o jsonpath='{.items[*].status.containerStatuses[0].ready}') == 'false' ]]; do
    echo "not-ready"
    sleep 30
  done

}

cluster_install_kubelet_config() {
  # Copy the docker config.json file into the kind-control-plane container
  if [ -f "${LOCAL_DOCKER_CONFIG_JSON}" ]
  then
    docker cp "${LOCAL_DOCKER_CONFIG_JSON}" "${KIND_CLUSTER_NAME}-control-plane:/var/lib/kubelet/config.json"
  else
    echo "NO ${HOME}/.docker/config.json: not configuring kind for external docker registry access"
  fi
}

create_ssl_cert_secret () {
  # generate CA and ingress certs
  ./load-or-generate-secret.sh $NAMESPACE "opennms-ca" "root-ca-certificate" "tmp/ca.key" "tmp/ca.crt"
  ./generate-and-sign-certificate.sh $NAMESPACE "minion.$DOMAIN" "opennms-minion-gateway-certificate" "tmp/ca.key" "tmp/ca.crt"
  ./generate-and-sign-certificate.sh $NAMESPACE "$DOMAIN" "opennms-ui-certificate" "tmp/ca.key" "tmp/ca.crt"

  # Generate client CA certificate
  ./load-or-generate-secret.sh $NAMESPACE "client-ca" "client-root-ca-certificate" "tmp/client-ca.key" "tmp/client-ca.crt"
}

# WHEN kind fixes the bug, https://github.com/kubernetes-sigs/kind/issues/3063,
# THEN changing this to load multiple images in a single command can save a huge amount of data transfer and time
load_images_to_kind_using_slow_kind () {
    kind load docker-image --name "$KIND_CLUSTER_NAME" "${IMAGE_PREFIX}/lokahi-alert:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-datachoices:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-events:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-grafana:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-inventory:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-keycloak:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-metrics-processor:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-minion:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-minion-gateway:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-minion-certificate-manager:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-minion-certificate-verifier:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-notification:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-rest-server:${IMAGE_TAG}" \
      "${IMAGE_PREFIX}/lokahi-ui:${IMAGE_TAG}"
}

pull_docker_images () {
	for image in \
		"${IMAGE_PREFIX}/lokahi-alert:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-datachoices:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-events:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-grafana:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-inventory:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-keycloak:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-metrics-processor:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-minion-certificate-manager:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-minion-certificate-verifier:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-minion:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-minion-gateway:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-notification:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-rest-server:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-ui:${IMAGE_TAG}"
	do
		if docker inspect "${image}" >/dev/null
		then
			echo "Already have ${image} locally"
		else
			docker pull "${image}"
		fi
	done
}

save_part_of_normal_docker_image_load () {
	docker save \
		"${IMAGE_PREFIX}/lokahi-alert:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-datachoices:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-events:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-grafana:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-inventory:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-keycloak:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-metrics-processor:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-minion-gateway:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-minion-certificate-manager:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-minion-certificate-verifier:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-minion:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-notification:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-rest-server:${IMAGE_TAG}" \
		"${IMAGE_PREFIX}/lokahi-ui:${IMAGE_TAG}"
}

load_part_of_normal_docker_image_load () {
	docker exec -i "${KIND_CLUSTER_NAME}-control-plane" ctr --namespace="${NAMESPACE}" images import --snapshotter overlayfs -
}

load_images_to_kind_using_normal_docker () {
	# Pull the images in case they are not yet available locally
	pull_docker_images

	### DEBUGGING
	echo =====
	docker images || crictl images || true
	echo =====

	save_part_of_normal_docker_image_load | load_part_of_normal_docker_image_load
}

create_namespace () {
  kubectl create namespace $NAMESPACE
}

install_helm_chart_custom_images () {
  echo
  echo ________________Installing Horizon Stream________________
  echo

  helm upgrade -i lokahi ./../charts/lokahi \
  -f ./tmp/install-local-opennms-lokahi-custom-images-values.yaml \
  --namespace $NAMESPACE \
  --set OpenNMS.Alert.Image=${IMAGE_PREFIX}/lokahi-alert:${IMAGE_TAG} \
  --set OpenNMS.DataChoices.Image=${IMAGE_PREFIX}/lokahi-datachoices:${IMAGE_TAG} \
  --set OpenNMS.Events.Image=${IMAGE_PREFIX}/lokahi-events:${IMAGE_TAG} \
  --set Grafana.Image=${IMAGE_PREFIX}/lokahi-grafana:${IMAGE_TAG} \
  --set OpenNMS.Inventory.Image=${IMAGE_PREFIX}/lokahi-inventory:${IMAGE_TAG} \
  --set Keycloak.Image=${IMAGE_PREFIX}/lokahi-keycloak:${IMAGE_TAG} \
  --set OpenNMS.MetricsProcessor.Image=${IMAGE_PREFIX}/lokahi-metrics-processor:${IMAGE_TAG} \
  --set OpenNMS.Minion.Image=${IMAGE_PREFIX}/lokahi-minion:${IMAGE_TAG} \
  --set OpenNMS.MinionGateway.Image=${IMAGE_PREFIX}/lokahi-minion-gateway:${IMAGE_TAG} \
  --set OpenNMS.MinionCertificateManager.Image=${IMAGE_PREFIX}/lokahi-minion-certificate-manager:${IMAGE_TAG} \
  --set OpenNMS.MinionCertificateVerifier.Image=${IMAGE_PREFIX}/lokahi-minion-certificate-verifier:${IMAGE_TAG} \
  --set OpenNMS.Notification.Image=${IMAGE_PREFIX}/lokahi-notification:${IMAGE_TAG} \
  --set OpenNMS.API.Image=${IMAGE_PREFIX}/lokahi-rest-server:${IMAGE_TAG} \
  --set OpenNMS.UI.Image=${IMAGE_PREFIX}/lokahi-ui:${IMAGE_TAG} \
  --wait --timeout "${TIMEOUT}"

  echo Helm chart installation completed
}

#### MAIN
################################

# LOG some useful info

echo "STARTUP CONFIG"
echo "CONTEXT=${CONTEXT}"
echo "DOMAIN=${DOMAIN}"
echo "IMAGE_TAG=${IMAGE_TAG}"
echo "IMAGE_PREFIX=${IMAGE_PREFIX}"
echo "KIND_CLUSTER_NAME=${KIND_CLUSTER_NAME}"
echo "NAMESPACE=${NAMESPACE}"

# Swap Domain in YAML files
mkdir -p tmp
cat install-local-onms-instance.yaml | \
  sed "s/onmshs/$DOMAIN/g" | sed "s/\$NAMESPACE/$NAMESPACE/g" > tmp/install-local-onms-instance.yaml
cat install-local-onms-instance-custom-images.yaml | \
  sed "s/onmshs/$DOMAIN/g" | sed "s/\$NAMESPACE/$NAMESPACE/g" > tmp/install-local-onms-instance-custom-images.yaml
cat ./../charts/lokahi/values.yaml | \
  sed "s/onmshs/$DOMAIN/g" | sed "s/\$NAMESPACE/$NAMESPACE/g" > tmp/values.yaml
cat install-local-opennms-lokahi-values.yaml | \
  sed "s/onmshs/$DOMAIN/g" | sed "s/\$NAMESPACE/$NAMESPACE/g" > tmp/install-local-opennms-lokahi-values.yaml
cat install-local-opennms-lokahi-custom-images-values.yaml | \
  sed "s/onmshs/$DOMAIN/g" | sed "s/\$NAMESPACE/$NAMESPACE/g" > tmp/install-local-opennms-lokahi-custom-images-values.yaml

# Select Context, Create Cluster, and Deploy
if [ $CONTEXT == "local" ]; then

  create_cluster
  cluster_install_kubelet_config
  create_namespace
  create_ssl_cert_secret

  echo
  echo ________________Installing Horizon Stream________________
  echo
  helm upgrade -i lokahi ./../charts/lokahi -f ./tmp/install-local-opennms-lokahi-values.yaml --namespace $NAMESPACE --wait --timeout "${TIMEOUT}"
  if [ $? -ne 0 ]; then exit; fi

  cluster_ready_check

elif [ "$CONTEXT" == "custom-images" ]; then

  create_cluster
  cluster_install_kubelet_config
  create_namespace
  create_ssl_cert_secret

  # Will add a kind-registry here at some point, see .github/ for sample script.
  echo "START LOADING IMAGES INTO KIND AT $(date)"

  time load_images_to_kind_using_normal_docker

  echo "FINISHED LOADING IMAGES INTO KIND AT $(date)"

  install_helm_chart_custom_images

  if [ $? -ne 0 ]; then exit; fi

  cluster_ready_check

elif [ "$CONTEXT" == "cicd" ]; then

  create_cluster
  cluster_install_kubelet_config
  create_namespace
  create_ssl_cert_secret

  # assumes remote docker registry, no need to load images into cluster
  install_helm_chart_custom_images

  # output values from the release to help with debugging pipelines
  helm get values lokahi --namespace $NAMESPACE

  if [ $? -ne 0 ]; then exit; fi

  cluster_ready_check

elif [ $CONTEXT == "existing-k8s" ]; then

  echo
  echo ________________Installing Horizon Stream________________
  echo
  helm upgrade -i lokahi ./../charts/lokahi -f ./tmp/install-local-opennms-lokahi-values.yaml --namespace $NAMESPACE --create-namespace --wait --timeout "${TIMEOUT}"
  if [ $? -ne 0 ]; then exit; fi

  cluster_ready_check

else
  echo "$HELP"
fi
