#!/usr/bin/env bash
#use this script to install a basic version of OpenNMS Horizon Stream locally

set -euo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

LOCAL_DOCKER_CONFIG_JSON="${HOME}/.docker/config.json"

#### ENV VARS
################################

# For local, we can setup localhost as the default on port 8080 or something.
# Like Skaffold and Tilt. 
# This determines whether or not to import custom images or not.
HELP='Need to pass "local" parameter to script'
if [ $# -lt 2 ]; then
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
TIMEOUT=${TIMEOUT:-20m0s}

LOAD_IMAGES_USING_KIND=${LOAD_IMAGES_USING_KIND:-}
DEBUG_IMAGES=${DEBUG_IMAGES:-}

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

  echo ________________Installing cert-manager_________
  kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.2/cert-manager.yaml
  kubectl rollout status deployment cert-manager --namespace cert-manager
  kubectl rollout status deployment cert-manager-webhook --namespace cert-manager
}

cluster_ready_check () {
  kubectl rollout status -n $NAMESPACE -w --timeout=$TIMEOUT deployment ingress-nginx-controller
}

cluster_install_kubelet_config() {
  # Copy the docker config.json file into the kind-control-plane container
  if [ -f "${LOCAL_DOCKER_CONFIG_JSON}" ]
  then
    # https://kind.sigs.k8s.io/docs/user/private-registries/
    node_name="${KIND_CLUSTER_NAME}-control-plane"
    docker cp "${LOCAL_DOCKER_CONFIG_JSON}" "${node_name}:/var/lib/kubelet/config.json"
    docker exec "${node_name}" systemctl restart kubelet.service
  else
    echo "NO ${LOCAL_DOCKER_CONFIG_JSON}: not configuring kind for external docker registry access"
  fi
}

create_ssl_cert_secret () {
  # generate CA and ingress certs
  ./load-or-generate-secret.sh "opennms-ca" "root-ca-certificate" "tmp/ca.key" "tmp/ca.crt" -n "$NAMESPACE"
  ./generate-and-sign-certificate.sh "minion.$DOMAIN" "opennms-minion-gateway-certificate" "tmp/ca.key" "tmp/ca.crt" -n "$NAMESPACE"
  ./generate-and-sign-certificate.sh "$DOMAIN" "opennms-ui-certificate" "tmp/ca.key" "tmp/ca.crt" -n "$NAMESPACE"

  # Generate client CA certificate
  ./load-or-generate-secret.sh "client-ca" "client-root-ca-certificate" "tmp/client-ca.key" "tmp/client-ca.crt" -n "$NAMESPACE"
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
	if ! docker exec -i "${KIND_CLUSTER_NAME}-control-plane" ctr --namespace="${NAMESPACE}" images import --snapshotter overlayfs -; then
		echo "" >&2
		echo "$(basename $0): failed to import images into kind cluster." >&2
		echo "If you got the error 'ctr: image might be filtered out'," >&2
		echo "'ctr: failed to resolve rootfs', or another odd error about an image, it" >&2
		echo "might be due to trying to load amd64 images on an arm64 system or vice-versa." >&2
		echo "See: https://github.com/kubernetes-sigs/kind/issues/2772#issuecomment-1145111244" >&2
		echo "" >&2
		echo "Double-check that you are using the right image names." >&2
		echo "" >&2
		echo "Make sure all of your images are built for the same platform that your" >&2
		echo "cluster is running as. You can use this command to see the architecture" >&2
		echo "for an image: docker inspect --format='{{.Architecture}}' <image>" >&2
		echo "" >&2
		echo "Lastly, try setting LOAD_IMAGES_USING_KIND=1 to load the images a different way." >&2
		exit 1
	fi
}

load_images_to_kind_using_normal_docker () {
	# Pull the images in case they are not yet available locally
	pull_docker_images

	### DEBUGGING
	if [ -n "${DEBUG_IMAGES}" ]; then
		echo =====
		docker images || crictl images || true
		echo =====
	fi

	save_part_of_normal_docker_image_load | load_part_of_normal_docker_image_load
}

create_namespace () {
  kubectl create namespace $NAMESPACE
}

install_nginx () {
  # The controller.service.type=ClusterIP is because kind doesn't provide a LoadBalancer by default.
  # https://github.com/fluxcd/flux2/issues/2476#issuecomment-1051275654
  # https://github.com/kubernetes-sigs/kind/issues/2889#issuecomment-1321223613
  helm upgrade --install ingress-nginx ingress-nginx \
    --repo https://kubernetes.github.io/ingress-nginx \
    --version 4.7.0 \
    --namespace $NAMESPACE \
    --set controller.service.type=ClusterIP \
    --set controller.hostPort.enabled=true \
    --values=../tilt-ingress-nginx-values.yaml \
    --wait --timeout "${TIMEOUT}"
}

install_helm_chart_custom_images () {
  echo
  echo ________________Installing Lokahi________________
  echo

  install_nginx
  if ! time helm upgrade -i lokahi ./../charts/lokahi \
  -f ./tmp/install-local-opennms-lokahi-custom-images-values.yaml \
  --namespace $NAMESPACE \
  --set OpenNMS.global.image.repository=${IMAGE_PREFIX} \
  --set OpenNMS.global.image.tag=${IMAGE_TAG} \
  --wait --timeout "${TIMEOUT}"; then
    helm_debug
  fi

  echo Helm chart installation completed
}

helm_debug () {
	echo "$(basename $0): Helm install/upgrade failed to complete within timeout." >&2
	echo "Pod status:" >&2
	kubectl get pods --namespace $NAMESPACE >&2
	echo "" >&2
	echo "If you see a lot of pods in ErrImagePull or ImagePullBackOff status," >&2
	echo "there might have been problems loading images into Kind. If you loaded" >&2
	echo "local images, try setting the LOAD_IMAGES_USING_KIND environment variable" >&2
	echo "to anything as a workaround and let us know how it goes." >&2
	exit 1
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
  echo ________________Installing Lokahi________________
  echo
  install_nginx
  time helm upgrade -i lokahi ./../charts/lokahi -f ./tmp/install-local-opennms-lokahi-values.yaml --namespace $NAMESPACE --wait --timeout "${TIMEOUT}"

  cluster_ready_check

elif [ "$CONTEXT" == "custom-images" ]; then

  create_cluster
  cluster_install_kubelet_config
  create_namespace
  create_ssl_cert_secret

  # Will add a kind-registry here at some point, see .github/ for sample script.
  echo "START LOADING IMAGES INTO KIND AT $(date)"

  if [ -z "${LOAD_IMAGES_USING_KIND}" ]; then
    time load_images_to_kind_using_normal_docker
  else
    time load_images_to_kind_using_slow_kind
  fi

  echo "FINISHED LOADING IMAGES INTO KIND AT $(date)"

  install_helm_chart_custom_images

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

  cluster_ready_check

elif [ $CONTEXT == "existing-k8s" ]; then

  echo
  echo ________________Installing Lokahi________________
  echo
  install_nginx
  time helm upgrade -i lokahi ./../charts/lokahi -f ./tmp/install-local-opennms-lokahi-values.yaml --namespace $NAMESPACE --create-namespace --wait --timeout "${TIMEOUT}"

  cluster_ready_check

else
  echo "$HELP"
fi
