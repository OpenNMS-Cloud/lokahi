//go:build integration
// +build integration

/*
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package integration

//import (
//	"context"
//	"github.com/OpenNMS/opennms-operator/internal/handlers"
//	"github.com/OpenNMS/opennms-operator/internal/model/values"
//	"github.com/OpenNMS/opennms-operator/internal/reconciler"
//	"github.com/OpenNMS/opennms-operator/internal/scheme"
//	"github.com/mittwald/go-helm-client"
//	"github.com/stretchr/testify/assert"
//	v1 "k8s.io/api/apps/v1"
//	batchv1 "k8s.io/api/batch/v1"
//	"k8s.io/apimachinery/pkg/types"
//	"path/filepath"
//	"reflect"
//	"sigs.k8s.io/controller-runtime/pkg/client"
//	"sigs.k8s.io/controller-runtime/pkg/envtest"
//	"testing"
//	"time"
//)
//
//func TestIntegration(t *testing.T) {
//	testenv := envtest.Environment{
//		CRDDirectoryPaths: []string{filepath.Join("charts", "opennms-operator", "crd")},
//	}
//	cfg, err := testenv.Start()
//	assert.Nil(t, err, "init test env")
//	opts := client.Options{Scheme: scheme.GetScheme()}
//	k8sClient, err := client.New(cfg, opts)
//	assert.Nil(t, err, "init k8s client")
//
//	helmOptions := &helmclient.RestConfClientOptions{
//		RestConfig: cfg,
//		Options:    &helmclient.Options{},
//	}
//	helmClient, err := helmclient.NewClientFromRestConf(helmOptions)
//	assert.Nil(t, err, "init helm client")
//
//	ctx := context.Background()
//
//	DeployOperatorAndCRD(t, ctx, helmClient)
//
//	DeployInstance(t, ctx, k8sClient)
//
//	RunIntegrationTests(t, ctx, k8sClient)
//}
//
//func RunIntegrationTests(t *testing.T, ctx context.Context, k8sClient client.Client) {
//	testReconciler := reconciler.OpenNMSReconciler{}
//	testReconciler.InitServiceHandlers()
//
//	handlers := testReconciler.Handlers
//
//	timeLimitExceeded := false
//
//	go func(t *testing.T) {
//		time.Sleep(5 * time.Minute)
//		assert.True(t, false, "time limit exceeded")
//		timeLimitExceeded = true
//	}(t)
//
//	for {
//		if timeLimitExceeded {
//			return
//		}
//		done := CheckResources(ctx, handlers, k8sClient)
//		if done {
//			break
//		} else {
//			time.Sleep(10 * time.Second)
//		}
//	}
//}
//
//func CheckResources(ctx context.Context, handlers []handlers.ServiceHandler, k8sClient client.Client) bool {
//	values := values.TemplateValues{
//		Release: values.HelmRelease{
//			Namespace: "test-instance",
//		},
//	}
//	for _, handler := range handlers {
//		for _, resource := range handler.ProvideConfig(values) {
//			deployedResource := resource.DeepCopyObject().(client.Object)
//			err := k8sClient.Get(ctx, types.NamespacedName{Name: resource.GetName(), Namespace: resource.GetNamespace()}, deployedResource)
//			kind := reflect.ValueOf(resource).Elem().Type().String()
//			if err != nil {
//				return false
//			} else {
//
//				switch kind {
//				case "v1.Deployment":
//					deployment := deployedResource.(*v1.Deployment)
//					if !(deployment.Status.ReadyReplicas == deployment.Status.Replicas) {
//						return false
//					}
//				case "v1.StatefulSet":
//					statefulset := deployedResource.(*v1.StatefulSet)
//					if !(statefulset.Status.ReadyReplicas == statefulset.Status.Replicas) {
//						return false
//					}
//				case "v1.Job":
//					job := deployedResource.(*batchv1.Job)
//					if !(job.Status.Succeeded == 1) {
//						return false
//					}
//				}
//			}
//		}
//	}
//	return true
//}
