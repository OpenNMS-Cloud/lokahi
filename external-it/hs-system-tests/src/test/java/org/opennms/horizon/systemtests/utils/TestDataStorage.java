package org.opennms.horizon.systemtests.utils;

import org.opennms.horizon.systemtests.keyvalue.SecretsStorage;

public class TestDataStorage {
    public static String mapUserToEmail(String email) {
        return switch (email) {
            case "ADMIN" -> SecretsStorage.adminUserEmail;
            case "OKTA_USER" -> SecretsStorage.oktaUserEmail;
            default -> email;
        };
    }
}
