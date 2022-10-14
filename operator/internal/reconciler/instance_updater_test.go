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

package reconciler

import (
	"context"
	"github.com/OpenNMS/opennms-operator/api/v1alpha1"
	"github.com/OpenNMS/opennms-operator/internal/model/values"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"testing"
)

func TestUpdateValues(t *testing.T) {
	testName := "testName"
	testNamespace := "testNamespace"
	testHost := "testingHost"
	testValues := values.TemplateValues{
		Values: values.Values{
			Host: testHost,
		},
		Release: values.HelmRelease{
			Namespace: testNamespace,
		},
	}

	k8sClient := fake.NewClientBuilder().Build()
	crd := v1alpha1.OpenNMS{
		ObjectMeta: v1.ObjectMeta{
			Name: testName,
		},
		Spec: v1alpha1.OpenNMSSpec{
			Namespace: testNamespace,
			Host:      testHost,
		},
	}
	testInstance := Instance{
		Values: testValues,
		CRD:    crd,
		Client: k8sClient,
	}

	testInstance.SetValues(context.Background())

	res := testInstance.Values

	assert.Equal(t, testNamespace, res.Release.Namespace, "should have populated values from reconcile request")
	assert.Equal(t, "testingHost", res.Values.Host, "should have used values from the default values")
}

func TestCheckForExistingCoreCreds(t *testing.T) {
	testKeycloakService := "onms-keycloak"
	testValues := values.TemplateValues{
		Values: values.Values{
			Keycloak: values.KeycloakValues{
				ServiceValues: values.ServiceValues{
					ServiceName: testKeycloakService,
				},
			},
		},
	}
	k8sClient := fake.NewClientBuilder().Build()
	testInstance := Instance{
		Values: testValues,
		Client: k8sClient,
	}
	ctx := context.Background()
	_, resbool := testInstance.CheckForExistingCoreCreds(ctx, testValues, "")
	assert.False(t, resbool, "should return that no core creds existed")

	_, resbool = testInstance.CheckForExistingPostgresCreds(ctx, testValues, "")
	assert.False(t, resbool, "should return that no postgres creds existed")

	adminPwd := "testadminpwd"
	coreSecret := corev1.Secret{
		ObjectMeta: v1.ObjectMeta{
			Name: "onms-keycloak-initial-admin",
		},
		Data: map[string][]byte{
			"password": []byte(adminPwd),
		},
	}
	err := k8sClient.Create(ctx, &coreSecret)
	assert.Nil(t, err)

	res, resbool := testInstance.CheckForExistingCoreCreds(ctx, testValues, "")
	assert.True(t, resbool, "should return that there are existing creds")
	assert.Equal(t, adminPwd, res.Values.Keycloak.AdminPassword, "should return the expected admin password values")

	adminPglPwd := "testpostgresadminpwd"
	keycloakPwd := "testpostgreskeycloakpwd"
    inventoryPwd := "testpostgresinventorypwd"
	notificationPwd := "testpostgresnotificationpwd"
	pgSecret := corev1.Secret{
		ObjectMeta: v1.ObjectMeta{
			Name: "postgres",
		},
		Data: map[string][]byte{
			"adminPwd":        []byte(adminPglPwd),
			"keycloakPwd":     []byte(keycloakPwd),
			"inventoryPwd":    []byte(inventoryPwd),
			"notificationPwd": []byte(notificationPwd),
		},
	}
	err = k8sClient.Create(ctx, &pgSecret)
	assert.Nil(t, err)

	res, resbool = testInstance.CheckForExistingPostgresCreds(ctx, testValues, "")
	assert.True(t, resbool, "should return that there are existing creds")
	assert.Equal(t, adminPglPwd, res.Values.Postgres.AdminPassword, "should return the postgres expected values")
	assert.Equal(t, keycloakPwd, res.Values.Postgres.KeycloakPassword, "should return the postgres expected values")
	assert.Equal(t, inventoryPwd, res.Values.Postgres.InventoryPassword, "should return the postgres expected values")
	assert.Equal(t, notificationPwd, res.Values.Postgres.NotificationPassword, "should return the postgres expected values")
}
