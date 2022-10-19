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
    valuesutil "github.com/OpenNMS/opennms-operator/internal/util/crd"
    "github.com/OpenNMS/opennms-operator/internal/util/security"
    v1 "k8s.io/api/core/v1"
    "k8s.io/apimachinery/pkg/types"
)

// SetValues - set values for an instance based on it's crd
func (i *Instance) SetValues(ctx context.Context) {
    namespace := i.CRD.Namespace

    templateValues := i.Values

    templateValues = valuesutil.ConvertCRDToValues(i.CRD, templateValues)

    // only set new passwords if they weren't already created by a previous operator
    templateValues, existingCreds := i.CheckForExistingCoreCreds(ctx, templateValues, namespace)
    if !existingCreds { // only set new passwords if they weren't already created by a previous operator
        templateValues = setCorePasswords(templateValues, i.CRD.Spec.Credentials)
    }

    templateValues, existingCreds = i.CheckForExistingPostgresCreds(ctx, templateValues, namespace)
    if !existingCreds {
        templateValues = setPostgresPassword(templateValues)
    }

    i.Values = templateValues
}

// CheckForExistingCoreCreds - checks if core credentials already exist for a given namespace
func (i *Instance) CheckForExistingCoreCreds(ctx context.Context, v values.TemplateValues, namespace string) (values.TemplateValues, bool) {
    var credSecret v1.Secret

    err := i.Client.Get(ctx, types.NamespacedName{Namespace: namespace, Name: v.Values.Keycloak.ServiceName + "-initial-admin"}, &credSecret)
    if err != nil {
        return v, false
    }

    existingAdminPwd := string(credSecret.Data["password"])
    if existingAdminPwd == "" {
        return v, false
    }
    v.Values.Keycloak.AdminPassword = existingAdminPwd

    return v, true
}

// CheckForExistingPostgresCreds - checks if core credentials already exist for a given namespace
func (i *Instance) CheckForExistingPostgresCreds(ctx context.Context, v values.TemplateValues, namespace string) (values.TemplateValues, bool) {
    var credSecret v1.Secret
    err := i.Client.Get(ctx, types.NamespacedName{Namespace: namespace, Name: "postgres"}, &credSecret)
    if err != nil {
        return v, false
    }
    adminPwd := string(credSecret.Data["adminPwd"])
    keycloakPwd := string(credSecret.Data["keycloakPwd"])
    notificationPwd := string(credSecret.Data["notificationPwd"])
    grafanaPwd := string(credSecret.Data["grafanaPwd"])
    if adminPwd == "" || keycloakPwd == "" || notificationPwd == "" {
        return v, false
    }
    v.Values.Postgres.AdminPassword = adminPwd
    v.Values.Postgres.KeycloakPassword = keycloakPwd
    v.Values.Postgres.NotificationPassword = notificationPwd
    v.Values.Postgres.GrafanaPassword = grafanaPwd
    return v, true
}

// setCorePasswords - sets randomly generated passwords for the core if not already set
func setCorePasswords(tv values.TemplateValues, creds v1alpha1.Credentials) values.TemplateValues {
    if creds.AdminPassword == "" {
        tv.Values.Keycloak.AdminPassword = security.GeneratePassword(true)
    } else {
        tv.Values.Keycloak.AdminPassword = creds.AdminPassword
    }

    return tv
}

// setPostgresPassword - sets randomly generated password for Postgres if not already set
func setPostgresPassword(tv values.TemplateValues) values.TemplateValues {
    tv.Values.Postgres.AdminPassword = security.GeneratePassword(true)
    tv.Values.Postgres.OpenNMSPassword = security.GeneratePassword(true)
    tv.Values.Postgres.KeycloakPassword = security.GeneratePassword(true)
    tv.Values.Postgres.NotificationPassword = security.GeneratePassword(true)

    //fed into Grafana via an .ini, so cannot generate with special characters
    tv.Values.Postgres.GrafanaPassword = security.GeneratePassword(false)
    return tv
}
