//go:build unit
// +build unit

package security

import (
	"github.com/stretchr/testify/assert"
	"strings"
	"testing"
)

func TestGeneratePassword(t *testing.T) {
	res := GeneratePassword(true)
	assert.True(t, strings.ContainsAny(res, Numbers), "should contain number characters")
	assert.True(t, strings.ContainsAny(res, UpperChars), "should contain uppercase characters")
	assert.True(t, strings.ContainsAny(res, SpecialChars), "should contain special characters")

	res = GeneratePassword(false)
	assert.False(t, strings.ContainsAny(res, SpecialChars), "should not contain any special characters")
}
