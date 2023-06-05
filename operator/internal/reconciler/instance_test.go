//go:build unit

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

package reconciler

import (
	"context"
	"github.com/OpenNMS-Cloud/opennms-operator/api/v1alpha1"
	"github.com/OpenNMS-Cloud/opennms-operator/config"
	"github.com/OpenNMS-Cloud/opennms-operator/internal/handlers"
	"github.com/OpenNMS-Cloud/opennms-operator/internal/handlers/base"
	values "github.com/OpenNMS-Cloud/opennms-operator/internal/model/values"
	values2 "github.com/OpenNMS-Cloud/opennms-operator/internal/util/values"
	"github.com/stretchr/testify/assert"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"testing"
)

var valuesSet bool

var tvals values.TemplateValues

func DefaultTestValues() values.TemplateValues {
	if !valuesSet {
		tvals, _ = values2.GetDefaultValues(config.OperatorConfig{
			DefaultOpenNMSValuesFile: "./../../charts/lokahi/values.yaml",
		})
		valuesSet = true
	}
	return tvals
}

func TestInstance(t *testing.T) {
	handlers.ConfigFilePath = "./../../../charts/lokahi/templates/"
	i := Instance{}
	crd := v1alpha1.OpenNMS{}
	crd.SetName("test")
	crd.Spec.Namespace = "testns"
	client := fake.NewClientBuilder().Build()
	vals := DefaultTestValues()
	h := []handlers.ServiceHandler{
		&base.BaseHandler{},
	}
	err := i.Init(
		context.Background(),
		client,
		vals,
		h,
		crd,
	)
	assert.Nil(t, err)
	assert.Equal(t, "test", i.Name, "should set the name correctly")
	assert.Equal(t, "test", i.CRD.Name, "should set the crd correctly")

	assert.GreaterOrEqual(t, len(i.Handlers[0].GetConfig()), 1, "config on handler should have been set")

	err = i.Update(context.Background(), crd)
	assert.Nil(t, err)
	assert.Equal(t, "testns", i.CRD.Spec.Namespace, "crd spec should not have changed")

	crd.Spec.Namespace = "otherns"

	err = i.Update(context.Background(), crd)
	assert.Nil(t, err)
	assert.Equal(t, "otherns", i.CRD.Spec.Namespace, "crd spec should have been updated")
}
