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

import jakarta.persistence.AttributeConverter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;
import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EncryptAttributeConverter implements AttributeConverter<String, String> {
    private static final Logger log = LoggerFactory.getLogger(EncryptAttributeConverter.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int AUTH_TAG_LENGTH = 128;
    private static final int ENCRYPTION_KEY_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${inventory.encryption.key:}")
    private String encryptionKey;

    @PostConstruct
    public void init() {
        if (StringUtils.isBlank(encryptionKey) || encryptionKey.length() != ENCRYPTION_KEY_LENGTH) {
            throw new InventoryRuntimeException("Inventory Encryption Key should be exactly 32 characters in length");
        }
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        Cipher cipher = getCipher();
        SecretKey key = getSecretKey();
        GCMParameterSpec paramSpec = getNewRandomParamSpec();
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate(paramSpec.getIV().length + cipherText.length);
            byteBuffer.put(paramSpec.getIV());
            byteBuffer.put(cipherText);
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (IllegalBlockSizeException
                | BadPaddingException
                | InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            log.error("Failed to convert to database column, writing in cleartext...", e);
            return plainText;
        }
    }

    @Override
    public String convertToEntityAttribute(String cipherMessage) {
        Cipher cipher = getCipher();
        Key key = getSecretKey();
        byte[] cipherMessageBytes = Base64.getDecoder().decode(cipherMessage);
        try {
            AlgorithmParameterSpec paramSpec =
                    new GCMParameterSpec(AUTH_TAG_LENGTH, cipherMessageBytes, 0, GCM_IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
            byte[] plainText =
                    cipher.doFinal(cipherMessageBytes, GCM_IV_LENGTH, cipherMessageBytes.length - GCM_IV_LENGTH);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (InvalidKeyException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException
                | IllegalArgumentException
                | BadPaddingException e) {
            log.error("Failed to convert to entity attribute, reading encrypted value...", e);
            return cipherMessage;
        }
    }

    private Cipher getCipher() {
        try {
            return Cipher.getInstance(TRANSFORMATION);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new InventoryRuntimeException("Failed to get cipher", e);
        }
    }

    private SecretKey getSecretKey() {
        if (StringUtils.isEmpty(encryptionKey)) {
            throw new InventoryRuntimeException("Failed to get encryption key");
        }
        return new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
    }

    private GCMParameterSpec getNewRandomParamSpec() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        RANDOM.nextBytes(iv);
        return new GCMParameterSpec(AUTH_TAG_LENGTH, iv);
    }
}
