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
package org.opennms.horizon.minion.flows.parser.session;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.minion.flows.parser.InvalidPacketException;
import org.opennms.horizon.minion.flows.parser.IpfixUdpParser;
import org.opennms.horizon.minion.flows.parser.MissingTemplateException;
import org.opennms.horizon.minion.flows.parser.Netflow9UdpParser;
import org.opennms.horizon.minion.flows.parser.ie.Value;
import org.opennms.horizon.minion.flows.parser.ie.values.StringValue;

public class UdpSessionManagerTest {

    final InetSocketAddress localAddress1 = new InetSocketAddress("10.10.10.10", 10001);
    final InetSocketAddress localAddress2 = new InetSocketAddress("10.10.10.10", 10002);

    long observationId1 = 11111;

    final InetSocketAddress remoteAddress1 = new InetSocketAddress("10.10.10.20", 51001);
    final InetSocketAddress remoteAddress2 = new InetSocketAddress("10.10.10.20", 51002);

    long observationId2 = 22222;

    final InetSocketAddress remoteAddress3 = new InetSocketAddress("10.10.10.30", 51001);
    final InetSocketAddress remoteAddress4 = new InetSocketAddress("10.10.10.30", 51002);

    int templateId1 = 100;

    private Scope scope(String name, String value) {
        return new Scope() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public int length() {
                return 0;
            }

            @Override
            public Value<?> parse(Session.Resolver resolver, ByteBuf buffer)
                    throws InvalidPacketException, MissingTemplateException {
                return new StringValue(name, Optional.empty(), value);
            }
        };
    }

    private Field field(String name, String value) {
        return new Field() {
            @Override
            public int length() {
                return 0;
            }

            @Override
            public Value<?> parse(Session.Resolver resolver, ByteBuf buffer)
                    throws InvalidPacketException, MissingTemplateException {
                return new StringValue(name, Optional.empty(), value);
            }
        };
    }

    private Value<?> value(String name, String value) {
        return new StringValue(name, Optional.empty(), value);
    }

    private void testNetflow9SessionKeys(
            final InetSocketAddress remote1,
            final InetSocketAddress local1,
            final InetSocketAddress remote2,
            final InetSocketAddress local2,
            final boolean shouldMatch) {
        final UdpSessionManager.SessionKey sessionKey1 = new Netflow9UdpParser.SessionKey(remote1.getAddress(), local1);
        final UdpSessionManager.SessionKey sessionKey2 = new Netflow9UdpParser.SessionKey(remote2.getAddress(), local2);
        testSessionKeys(sessionKey1, sessionKey2, shouldMatch);
    }

    private void testIpFixSessionKeys(
            final InetSocketAddress remote1,
            final InetSocketAddress local1,
            final InetSocketAddress remote2,
            final InetSocketAddress local2,
            final boolean shouldMatch) {
        final UdpSessionManager.SessionKey sessionKey1 = new IpfixUdpParser.SessionKey(remote1, local1);
        final UdpSessionManager.SessionKey sessionKey2 = new IpfixUdpParser.SessionKey(remote2, local2);
        testSessionKeys(sessionKey1, sessionKey2, shouldMatch);
    }

    private void testSessionKeys(
            final UdpSessionManager.SessionKey sessionKey1,
            final UdpSessionManager.SessionKey sessionKey2,
            final boolean shouldMatch) {
        final UdpSessionManager udpSessionManager =
                new UdpSessionManager(Duration.ofMinutes(30), () -> new SequenceNumberTracker(32));
        final Session session1 = udpSessionManager.getSession(sessionKey1);

        final List<Scope> scopes = new ArrayList<>();
        scopes.add(scope("scope1", null));
        scopes.add(scope("scope2", null));

        final List<Field> fields = new ArrayList<>();
        fields.add(field("field1", null));
        fields.add(field("field2", null));

        final Template template = Template.builder(100, Template.Type.OPTIONS_TEMPLATE)
                .withFields(fields)
                .withScopes(scopes)
                .build();
        session1.addTemplate(observationId1, template);

        final List<Value<?>> scopesValue = new ArrayList<>();
        scopesValue.add(value("scope1", "scopeValue1"));
        scopesValue.add(value("scope2", "scopeValue2"));

        final List<Value<?>> fieldsValue = new ArrayList<>();
        fieldsValue.add(value("additionalField1", "additionalValue1"));
        fieldsValue.add(value("additionalField2", "additionalValue2"));

        session1.addOptions(observationId1, templateId1, scopesValue, fieldsValue);

        final Session session2 = udpSessionManager.getSession(sessionKey2);

        final List<Value<?>> notMatchingValues = new ArrayList<>();
        notMatchingValues.add(value("scope1", "scopeValue1"));
        notMatchingValues.add(value("scope2", "mismatch"));

        final List<Value<?>> matchingValues = new ArrayList<>();
        matchingValues.add(value("scope1", "scopeValue1"));
        matchingValues.add(value("scope2", "scopeValue2"));

        Assert.assertEquals(
                0,
                session2.getResolver(observationId1)
                        .lookupOptions(notMatchingValues)
                        .size());
        Assert.assertEquals(
                0,
                session2.getResolver(observationId2)
                        .lookupOptions(matchingValues)
                        .size());

        final List<Value<?>> result = session2.getResolver(observationId1).lookupOptions(matchingValues);

        System.out.println("Checking session keys " + sessionKey1 + " and " + sessionKey2);
        Assert.assertEquals(shouldMatch ? 2 : 0, result.size());
        Assert.assertEquals(
                shouldMatch,
                result.contains(new StringValue("additionalField1", Optional.empty(), "additionalValue1")));
        Assert.assertEquals(
                shouldMatch,
                result.contains(new StringValue("additionalField2", Optional.empty(), "additionalValue2")));
    }

    /**
     * see NMS-13539
     */
    @Test
    public void optionsRemovalTest() {
        final UdpSessionManager.SessionKey sessionKey =
                new Netflow9UdpParser.SessionKey(remoteAddress1.getAddress(), localAddress1);

        final UdpSessionManager udpSessionManager =
                new UdpSessionManager(Duration.ofMinutes(0), () -> new SequenceNumberTracker(32));
        final Session session = udpSessionManager.getSession(sessionKey);

        final List<Scope> scopes = new ArrayList<>();
        scopes.add(scope("scope1", null));
        scopes.add(scope("scope2", null));

        final List<Field> fields = new ArrayList<>();
        fields.add(field("field1", null));
        fields.add(field("field2", null));

        final Template template = Template.builder(100, Template.Type.OPTIONS_TEMPLATE)
                .withFields(fields)
                .withScopes(scopes)
                .build();
        session.addTemplate(observationId1, template);

        final List<Value<?>> scopesValue = new ArrayList<>();
        scopesValue.add(value("scope1", "scopeValue1"));
        scopesValue.add(value("scope2", "scopeValue2"));

        final List<Value<?>> fieldsValue = new ArrayList<>();
        fieldsValue.add(value("additionalField1", "additionalValue1"));
        fieldsValue.add(value("additionalField2", "additionalValue2"));

        session.addOptions(observationId1, templateId1, scopesValue, fieldsValue);

        assertThat(
                udpSessionManager.templates.keySet(),
                hasItem(new UdpSessionManager.TemplateKey(sessionKey, observationId1, template.id)));
        assertThat(
                udpSessionManager
                        .templates
                        .get(new UdpSessionManager.TemplateKey(sessionKey, observationId1, template.id))
                        .wrapped
                        .options
                        .entrySet(),
                not(empty()));

        udpSessionManager.doHousekeeping();

        assertThat(
                udpSessionManager.templates.keySet(),
                not(hasItem(new UdpSessionManager.TemplateKey(sessionKey, observationId1, template.id))));
        assertThat(
                udpSessionManager.templates.get(
                        new UdpSessionManager.TemplateKey(sessionKey, observationId1, template.id)),
                nullValue());
    }

    @Test
    public void testNetflow9() {
        testNetflow9SessionKeys(remoteAddress1, localAddress1, remoteAddress1, localAddress1, true);
        testNetflow9SessionKeys(remoteAddress1, localAddress1, remoteAddress1, localAddress2, false);
        // this should match, since Netflow v9 session keys do not include the remote port, see NMS-10721
        testNetflow9SessionKeys(remoteAddress1, localAddress1, remoteAddress2, localAddress1, true);
        testNetflow9SessionKeys(remoteAddress1, localAddress1, remoteAddress2, localAddress2, false);
        testNetflow9SessionKeys(remoteAddress1, localAddress1, remoteAddress3, localAddress1, false);
        testNetflow9SessionKeys(remoteAddress1, localAddress1, remoteAddress3, localAddress2, false);
        testNetflow9SessionKeys(remoteAddress1, localAddress1, remoteAddress4, localAddress1, false);
        testNetflow9SessionKeys(remoteAddress1, localAddress1, remoteAddress4, localAddress2, false);
    }

    @Test
    public void testIpFix() {
        testIpFixSessionKeys(remoteAddress1, localAddress1, remoteAddress1, localAddress1, true);
        testIpFixSessionKeys(remoteAddress1, localAddress1, remoteAddress1, localAddress2, false);
        testIpFixSessionKeys(remoteAddress1, localAddress1, remoteAddress2, localAddress1, false);
        testIpFixSessionKeys(remoteAddress1, localAddress1, remoteAddress2, localAddress2, false);
        testIpFixSessionKeys(remoteAddress1, localAddress1, remoteAddress3, localAddress1, false);
        testIpFixSessionKeys(remoteAddress1, localAddress1, remoteAddress3, localAddress2, false);
        testIpFixSessionKeys(remoteAddress1, localAddress1, remoteAddress4, localAddress1, false);
        testIpFixSessionKeys(remoteAddress1, localAddress1, remoteAddress4, localAddress2, false);
    }
}
