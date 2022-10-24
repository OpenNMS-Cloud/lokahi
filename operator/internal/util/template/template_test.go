//go:build unit
// +build unit

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

package template

import (
	"github.com/OpenNMS/opennms-operator/internal/model/values"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestTemplate(t *testing.T) {
	namespace := "testNamespace"
	testString := "{{ .Release.Namespace }}"
	v := values.TemplateValues{
		Release: values.HelmRelease{
			Namespace: namespace,
		},
	}
	res, err := TemplateConfig(testString, v)
	assert.Nil(t, err)
	assert.Equal(t, namespace, res, "should template correctly")

	testString = "<<<>>>"

	res, err = TemplateConfig(testString, v)
	assert.Nil(t, err)
	assert.Equal(t, testString, res, "should not encode lessthan symbols")
}
