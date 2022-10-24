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

package yaml

import (
	"github.com/OpenNMS/opennms-operator/internal/model/values"
	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/apps/v1"
	"os"
	"path/filepath"
	"testing"
)

var TestFilename = "./testtmp/test1.yaml"

var TestFileContents = `
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: test
  name: test
  namespace: {{ .Release.Namespace }}
`

func TestLoadYaml(t *testing.T) {
	writeTestFiles(t)
	values := values.TemplateValues{
		Release: values.HelmRelease{
			Namespace: "testNamespace",
		},
	}
	testInterface := v1.Deployment{}
	LoadYaml(TestFilename, values, &testInterface)
	assert.Equal(t, "test", testInterface.ObjectMeta.Name, "should have loaded the correct name from the yaml")
	assert.Equal(t, "testNamespace", testInterface.ObjectMeta.Namespace, "should have templated in the correct provided namespace")

	cachedFile, ok := Cache().Get(TestFilename)
	assert.True(t, ok)
	assert.Equal(t, TestFileContents, cachedFile, "should have stored the untemplated file in the cache")
}

func writeTestFiles(t *testing.T) {
	file1 := []byte(TestFileContents)

	newpath := filepath.Join(".", "testtmp")
	err := os.MkdirAll(newpath, os.ModePerm)
	assert.Nil(t, err)

	err = os.WriteFile(TestFilename, file1, 0644)
	assert.Nil(t, err)
}
