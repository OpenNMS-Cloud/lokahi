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

package values

import (
	"github.com/stretchr/testify/assert"
	"os"
	"path/filepath"
	"testing"
)

func TestLoadValues(t *testing.T) {
	writeTestFiles(t)

	v := LoadValues(TestFilename1, TestFilename2)
	assert.NotNil(t, v)
	assert.Equal(t, false, v.TestDeploy, "should load the first yaml file correctly")
	assert.Equal(t, "testHost", v.Host, "should load the second yaml file correctly")

}

var TestFilename1 = "./testtmp/test1.yaml"
var TestFilename2 = "./testtmp/test2.yaml"

var TestFileContents1 = `TestDeploy: false`
var TestFileContents2 = `Host: testHost`

func writeTestFiles(t *testing.T) {
	file1 := []byte(TestFileContents1)
	file2 := []byte(TestFileContents2)

	newpath := filepath.Join(".", "testtmp")
	err := os.MkdirAll(newpath, os.ModePerm)
	assert.Nil(t, err)

	err = os.WriteFile(TestFilename1, file1, 0644)
	assert.Nil(t, err)

	err = os.WriteFile(TestFilename2, file2, 0644)
	assert.Nil(t, err)
}
