/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.springframework.test.util.ReflectionTestUtils;

class EncryptAttributeConverterTest {
    private static final int VALID_KEY_LENGTH = 32;
    private static final int INVALID_KEY_LENGTH = 29;
    private static final String ENCRYPTION_KEY_FIELD = "encryptionKey";

    private EncryptAttributeConverter converter;

    @BeforeEach
    public void before() {
        converter = new EncryptAttributeConverter();
    }

    @Test
    void testEncryptionKeyValidationBlankString() {
        ReflectionTestUtils.setField(converter, ENCRYPTION_KEY_FIELD, "  ");
        InventoryRuntimeException e = assertThrows(InventoryRuntimeException.class, () -> {
            converter.init();
        });
        assertEquals("Inventory Encryption Key should be exactly 32 characters in length", e.getMessage());
    }

    @Test
    void testEncryptionKeyValidationValidKeyLength() {
        ReflectionTestUtils.setField(
                converter, ENCRYPTION_KEY_FIELD, RandomStringUtils.randomAlphabetic(VALID_KEY_LENGTH));
        converter.init();
        assertTrue(true);
    }

    @Test
    void testEncryptionKeyValidationInvalidKeyLength() {
        ReflectionTestUtils.setField(
                converter, ENCRYPTION_KEY_FIELD, RandomStringUtils.randomAlphabetic(INVALID_KEY_LENGTH));
        InventoryRuntimeException e = assertThrows(InventoryRuntimeException.class, () -> {
            converter.init();
        });
        assertEquals("Inventory Encryption Key should be exactly 32 characters in length", e.getMessage());
    }

    @Test
    void testEncryptDecrypt() {
        ReflectionTestUtils.setField(
                converter, ENCRYPTION_KEY_FIELD, RandomStringUtils.randomAlphabetic(VALID_KEY_LENGTH));
        List<String> plaintextStrings = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            plaintextStrings.add(RandomStringUtils.randomAlphanumeric(RandomUtils.nextInt(5, 30)));
        }

        List<String> encryptedStrings = new ArrayList<>();
        for (String plainText : plaintextStrings) {
            encryptedStrings.add(converter.convertToDatabaseColumn(plainText));
        }

        for (int index = 0; index < 10; index++) {
            String plaintext = plaintextStrings.get(index);
            String decryptedText = converter.convertToEntityAttribute(encryptedStrings.get(index));
            assertEquals(plaintext, decryptedText);
        }
    }

    @Test
    void testEncryptDecryptWithoutKey() {
        String plainText = RandomStringUtils.randomAlphanumeric(10);
        InventoryRuntimeException e = assertThrows(InventoryRuntimeException.class, () -> {
            converter.convertToDatabaseColumn(plainText);
        });
        assertEquals("Failed to get encryption key", e.getMessage());
    }

    @Test
    void testEncryptInvalidKeyLength() {
        ReflectionTestUtils.setField(
                converter, ENCRYPTION_KEY_FIELD, RandomStringUtils.randomAlphabetic(INVALID_KEY_LENGTH));
        String plainText = RandomStringUtils.randomAlphanumeric(10);
        String converted = converter.convertToDatabaseColumn(plainText);
        assertEquals(plainText, converted);
    }

    @Test
    void testDecryptInvalidKeyLength() {
        ReflectionTestUtils.setField(
                converter, ENCRYPTION_KEY_FIELD, RandomStringUtils.randomAlphabetic(INVALID_KEY_LENGTH));
        String encryptedText = RandomStringUtils.randomAlphanumeric(10);
        String converted = converter.convertToEntityAttribute(encryptedText);
        assertEquals(encryptedText, converted);
    }
}
