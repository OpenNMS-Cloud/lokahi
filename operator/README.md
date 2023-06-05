![](https://www.opennms.com/wp-content/uploads/2021/04/OpenNMS_Horizontal-Logo_Light-BG-retina-website-300x56.png)

# OpenNMS Operator

A Kubernetes operator for deploying and maintaining [Horizon by OpenNMS](https://github.com/OpenNMS-Cloud/opennms) in the cloud.

### Versioning

This repository follows [Semantic Versioning](https://semver.org/)

### Quick install

[Docker](https://docs.docker.com/engine/install/), [kubectl](https://kubernetes.io/docs/tasks/tools/), and [Helm](https://helm.sh/docs/intro/install/) must all be installed. 
Additionally you'll need some sort of local Kubernetes cluster running on your machine, i.e. [Docker Desktop](https://docs.docker.com/desktop/kubernetes/), [minikube](https://minikube.sigs.k8s.io/docs/start/), [kind](https://kind.sigs.k8s.io/docs/user/quick-start/), etc.  


Make sure your kubectl is running at least version `1.23.3`. Upgrade it if not.

```aidl
% kubectl version
Client Version: version.Info{Major:"1", Minor:"23", GitVersion:"v1.23.3", GitCommit:"816c97ab8cff8a1c72eccca1026f7820e93e0d25", GitTreeState:"clean", BuildDate:"2022-01-25T21:17:57Z", GoVersion:"go1.17.6", Compiler:"gc", Platform:"darwin/arm64"}
Server Version: version.Info{Major:"1", Minor:"24", GitVersion:"v1.24.0", GitCommit:"4ce5a8954017644c5420bae81d72b09b735c21f0", GitTreeState:"clean", BuildDate:"2022-05-19T15:42:59Z", GoVersion:"go1.18.1", Compiler:"gc", Platform:"linux/arm64"}
```

Run the local installation script.
```
bash scripts/deploy-lokahi.sh
```

### OpenNMS on Openshift

#### Openshift Install and Login

Create an OpenShift account, login, and download the [Local installer from here](https://console.redhat.com/openshift/create/local).

Run the following:

```
crc setup
crc start -m 30000
```

`start -m 30000` will start the cluster with ~30GB of memory allocated to it. This can be adjusted, but OpenShift and OpenNMS need at least 20GB to be fully functional.

`start` will ask you for a pull secret, there's a button to copy it beside the OS download button in the OS console. 

Wait for `start` to finish, then run the following to login into your cluster:

```
eval $(crc oc-env)
crc console --credentials
```

Something like this will be printed:

```
% crc console --credentials
To login as a regular user, run 'oc login -u developer -p developer https://api.crc.testing:6443'.
To login as an admin, run 'oc login -u kubeadmin -p W7uLo-iWt5L-CtfnA-fjJCa https://api.crc.testing:6443'
```

Copy the `admin` line and run it:

```
% oc login -u kubeadmin -p W7uLo-iWt5L-CtfnA-fjJCa https://api.crc.testing:6443
Login successful.

You have access to 67 projects, the list has been suppressed. You can list all projects with 'oc projects'

Using project "default".
```

You can now interact with the OpenShift cluster using `kubectl`.

#### Installing OpenNMS Horizon Stream to Openshift

Run the following to build the Operator's docker image:

```
make local-docker
```

Then run the following commands: 

```
REGISTRY="$(oc get route/default-route -n openshift-image-registry -o=jsonpath='{.spec.host}')/openshift"
docker login -u kubeadmin -p $(oc whoami -t) $REGISTRY
docker tag opennms/operator:local-build $REGISTRY/opennms-operator:local
docker push $REGISTRY/opennms-operator:local
```

They extract the OS cluster's internal image registry URL, and pushes the built Docker image to it.

Then install the Operator to the cluster using Helm:

```
helm upgrade -i operator-local ../charts/opennms-operator -f scripts/openshift-operator-values.yaml --namespace opennms --create-namespace
```

And give it the following OpenShift permission, which allows its K8s Pod to pull from the above internal image registry:

```
oc adm policy add-role-to-user system:image-pullers system:serviceaccount:opennms-operator -n opennms
```

And then create the OpenNMS instance:

```
kubectl apply -f scripts/local-os-onms-instance.yaml
```

Run `kubectl get pods -n opennms` to see the instance starting. The instance will be finished starting once all Pods are `Running`

The instance will be accessible at `TODO_URL_HERE`

#### Macbook M1

If you have an M1 Macbook, a local install of OpenNMS on Openshift is not currently supported. Support will be added for Apple Silicon in the near future. 





