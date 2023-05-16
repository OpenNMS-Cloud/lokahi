/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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
 *
 */

package org.opennms.horizon.minion.grpc.channel;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Fairly basic verification of channel construction logic based on tls switch.
 */
@ExtendWith(MockitoExtension.class)
public class SecuritySwitchedChannelFactoryTest {

    @Mock(name = "plainText")
    ManagedChannelFactory plainText;

    @Mock(name = "secure")
    ManagedChannelFactory secure;

    SecuritySwitchedChannelFactory channelFactory;

    @BeforeEach
    public void setUp() {
        channelFactory = new SecuritySwitchedChannelFactory(plainText, secure);
    }

    @Test
    public void testTlsFactory() {
        channelFactory.setTlsEnabled(true);
        channelFactory.create("foo", 123, null);
        channelFactory.create("bar", 321, "foo");

        verify(secure).create("foo", 123, null);
        verify(secure).create("bar", 321, "foo");
        verifyNoInteractions(plainText);
    }

    @Test
    public void testPlainTextFactory() {
        channelFactory.setTlsEnabled(false);
        channelFactory.create("foo", 123, null);
        channelFactory.create("bar", 321, "foo");

        verify(plainText).create("foo", 123, null);
        verify(plainText).create("bar", 321, "foo");
        verifyNoInteractions(secure);
    }

}
