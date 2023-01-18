/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.horizon.inventory.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.persistence.AttributeConverter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptAttributeConverter implements AttributeConverter<String, String> {
    private static final Logger log = LoggerFactory.getLogger(EncryptAttributeConverter.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Key key;
    private Cipher cipher;
    private IvParameterSpec ivParams;

    @PostConstruct
    public void init() throws Exception {
        // todo: IMPORTANT change this. generate random and externalize
        String secret = randomString(16);

        this.key = new SecretKeySpec(secret.getBytes(), ALGORITHM);
        this.cipher = Cipher.getInstance(TRANSFORMATION);

        byte[] bytesIV = new byte[this.cipher.getBlockSize()];
        RANDOM.nextBytes(bytesIV);
        this.ivParams = new IvParameterSpec(bytesIV);
    }

    @Override
    public String convertToDatabaseColumn(String value) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
            return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException e) {
            log.error("Failed to convert to database column, writing in cleartext...", e);
            return value;
        }
    }

    @Override
    public String convertToEntityAttribute(String value) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
            return new String(cipher.doFinal(Base64.getDecoder().decode(value)));
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                 InvalidAlgorithmParameterException e) {
            log.error("Failed to convert to entity attribute, reading encrypted value...", e);
            return value;
        }
    }

    private static final String LETTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }
        return sb.toString();
    }
}
