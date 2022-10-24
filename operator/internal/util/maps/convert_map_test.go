//go:build unit
// +build unit

package maps

import (
    "github.com/stretchr/testify/assert"
    "testing"
)

func TestConvertByteToStringMap(t *testing.T) {
    input := map[string][]byte{
        "foo":  []byte("bar"),
        "test": []byte("tesssst"),
    }
    expected := map[string]string{
        "foo":  "bar",
        "test": "tesssst",
    }

    res := ConvertByteToStringMap(input)
    assert.Equal(t, expected, res, "should convert the map correctly")
}
