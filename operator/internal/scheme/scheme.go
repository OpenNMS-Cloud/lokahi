package scheme

import (
    opennmsv1alpha1 "github.com/OpenNMS-Cloud/opennms-operator/api/v1alpha1"
    routev1 "github.com/openshift/api/route/v1"
    "k8s.io/apimachinery/pkg/runtime"
    utilruntime "k8s.io/apimachinery/pkg/util/runtime"
    clientgoscheme "k8s.io/client-go/kubernetes/scheme"
)

// GetScheme - get the k8s scheme + any custom resources being used
func GetScheme() *runtime.Scheme {
    scheme := runtime.NewScheme()

    utilruntime.Must(clientgoscheme.AddToScheme(scheme))
    utilruntime.Must(opennmsv1alpha1.AddToScheme(scheme))
    utilruntime.Must(routev1.AddToScheme(scheme))

    return scheme
}
