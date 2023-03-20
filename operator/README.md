![](https://www.opennms.com/wp-content/uploads/2021/04/OpenNMS_Horizontal-Logo_Light-BG-retina-website-300x56.png)

# OpenNMS Operator

A Kubernetes operator for deploying and maintaining [Horizon by OpenNMS](https://github.com/OpenNMS-Cloud/opennms) in the cloud.

### Versioning

This repository follows [Semantic Versioning](https://semver.org/)

### Quick install

Have some sort of local Kubernetes cluster running on your machine, i.e. [Docker Desktop](https://docs.docker.com/desktop/kubernetes/), [minikube](https://minikube.sigs.k8s.io/docs/start/), [kind](https://kind.sigs.k8s.io/docs/user/quick-start/), etc.  

Make sure your kubectl is running at least version `1.23.3`. Upgrade it if not.

```aidl
% kubectl version
Client Version: version.Info{Major:"1", Minor:"23", GitVersion:"v1.23.3", GitCommit:"816c97ab8cff8a1c72eccca1026f7820e93e0d25", GitTreeState:"clean", BuildDate:"2022-01-25T21:17:57Z", GoVersion:"go1.17.6", Compiler:"gc", Platform:"darwin/arm64"}
Server Version: version.Info{Major:"1", Minor:"24", GitVersion:"v1.24.0", GitCommit:"4ce5a8954017644c5420bae81d72b09b735c21f0", GitTreeState:"clean", BuildDate:"2022-05-19T15:42:59Z", GoVersion:"go1.18.1", Compiler:"gc", Platform:"linux/arm64"}
```

Run the local installation script
```
bash deploy-horizon-stream.yaml
```

### OpenShift Install

Create an OpenShift account, login, and download the [Local installer from here](https://console.redhat.com/openshift/create/local).

Run the following:

```
crc setup
crc start
```

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



#### Macbook M1

If you have an M1 Macbook, follow these steps in addition to the above.

?????????????????????? 





