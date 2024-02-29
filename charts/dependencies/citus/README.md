

## Steps to run
### Dependency
```
kubectl create namespace lokahi
# Cert-manager
helm repo add jetstack https://charts.jetstack.io  --force-update
helm install cert-manager jetstack/cert-manager --version=1.11.0 --set installCRDs=true --set cainjector.extraArgs={--leader-elect=false} --namespace lokahi                                                              
```
### Install
```
helm dep build
helm install citus . 
```
### Uninstall
```
helm uninstall cert-manager -n lokahi
kubectl delete namespace lokahi
```