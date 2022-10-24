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
//	"github.com/OpenNMS/opennms-operator/api/v1alpha1"
//	"github.com/OpenNMS/opennms-operator/internal/model/values"
//	"github.com/OpenNMS/opennms-operator/internal/util/yaml"
//	helmclient "github.com/mittwald/go-helm-client"
//	"github.com/stretchr/testify/assert"
//	"sigs.k8s.io/controller-runtime/pkg/client"
//	"testing"
//	"time"
//)
//
//func DeployOperatorAndCRD(t *testing.T, ctx context.Context, helmClient helmclient.Client) {
//	chartSpec := &helmclient.ChartSpec{
//		ReleaseName: "opennms-operator-integration-test",
//		ChartName:   "charts/opennms-operator",
//		Wait:        true,
//		Timeout:     time.Second * 300,
//		UpgradeCRDs: true,
//	}
//	_, err := helmClient.InstallOrUpgradeChart(ctx, chartSpec)
//	assert.Nil(t, err, "deploy operator chart and crd")
//}
//
//func DeployInstance(t *testing.T, ctx context.Context, k8sClient client.Client) {
//	var testInstance v1alpha1.OpenNMS
//	yaml.LoadYaml("test/integration/test_instance.yaml", values.TemplateValues{}, &testInstance)
//
//	err := k8sClient.Create(ctx, &testInstance)
//	assert.Nil(t, err)
//}
