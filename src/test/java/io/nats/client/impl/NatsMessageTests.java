// Copyright 2015-2018 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client.impl;

import io.nats.client.*;
import io.nats.client.NatsServerProtocolMock.ExitAt;
import io.nats.client.support.IncomingHeadersProcessor;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static io.nats.client.utils.ResourceUtils.dataAsLines;
import static io.nats.client.utils.TestBase.assertByteArraysEqual;
import static io.nats.client.utils.TestBase.standardConnectionWait;
import static org.junit.jupiter.api.Assertions.*;

public class NatsMessageTests {
    @Test
    public void testSizeOnProtocolMessage() {
        NatsMessage msg = new NatsMessage.ProtocolMessage("PING");
        assertEquals(msg.getProtocolBytes().length + 2, msg.getSizeInBytes(), "Size is set, with CRLF");
        assertEquals("PING".getBytes(StandardCharsets.UTF_8).length + 2, msg.getSizeInBytes(), "Size is correct");
        assertTrue(msg.toString().endsWith("PING")); // toString COVERAGE
    }

    @Test
    public void testSizeOnPublishMessage() {
        byte[] body = new byte[10];
        String subject = "subj";
        String replyTo = "reply";
        String protocol = "PUB " + subject + " " + replyTo + " " + body.length;

        NatsMessage msg = new NatsMessage(subject, replyTo, body);

        assertEquals(msg.getProtocolBytes().length + body.length + 4, msg.getSizeInBytes(), "Size is set, with CRLF");
        assertEquals(protocol.getBytes(StandardCharsets.US_ASCII).length + body.length + 4, msg.getSizeInBytes(), "Size is correct");

        msg = new NatsMessage(subject, replyTo, body);

        assertEquals(msg.getProtocolBytes().length + body.length + 4, msg.getSizeInBytes(), "Size is set, with CRLF");
        assertEquals(protocol.getBytes(StandardCharsets.UTF_8).length + body.length + 4, msg.getSizeInBytes(), "Size is correct");
    }

    @Test
    public void testSizeOnPublishMessageWithHeaders() {
        Headers headers = new Headers().add("Content-Type", "text/plain");
        byte[] body = new byte[10];
        String subject = "subj";
        String replyTo = "reply";
        String protocol = "HPUB " + subject + " " + replyTo + " " + headers.serializedLength() + " " + (headers.serializedLength() + body.length);

        NatsMessage msg = new NatsMessage(subject, replyTo, headers, body);

        assertEquals(msg.getProtocolBytes().length + headers.serializedLength() + body.length + 4, msg.getSizeInBytes(), "Size is set, with CRLF");
        assertEquals(protocol.getBytes(StandardCharsets.US_ASCII).length + headers.serializedLength() + body.length + 4, msg.getSizeInBytes(), "Size is correct");

        msg = new NatsMessage(subject, replyTo, headers, body);

        assertEquals(msg.getProtocolBytes().length + headers.serializedLength() + body.length + 4, msg.getSizeInBytes(), "Size is set, with CRLF");
        assertEquals(protocol.getBytes(StandardCharsets.UTF_8).length + headers.serializedLength() + body.length + 4, msg.getSizeInBytes(), "Size is correct");
    }

    @Test
    public void testSizeOnPublishMessageOnlyHeaders() {
        Headers headers = new Headers().add("Content-Type", "text/plain");
        String subject = "subj";
        String replyTo = "reply";
        String protocol = "HPUB " + subject + " " + replyTo + " " + headers.serializedLength() + " " + headers.serializedLength();

        NatsMessage msg = new NatsMessage(subject, replyTo, headers, null);

        assertEquals(msg.getProtocolBytes().length + headers.serializedLength() + 4, msg.getSizeInBytes(), "Size is set, with CRLF");
        assertEquals(protocol.getBytes(StandardCharsets.US_ASCII).length + headers.serializedLength() + 4, msg.getSizeInBytes(), "Size is correct");

        msg = new NatsMessage(subject, replyTo, headers, null);

        assertEquals(msg.getProtocolBytes().length + headers.serializedLength() + 4, msg.getSizeInBytes(), "Size is set, with CRLF");
        assertEquals(protocol.getBytes(StandardCharsets.UTF_8).length + headers.serializedLength() + 4, msg.getSizeInBytes(), "Size is correct");
    }

    @Test
    public void testSizeOnPublishMessageOnlySubject() {
        String subject = "subj";
        String replyTo = "reply";
        String protocol = "PUB " + subject + " " + replyTo + " " + 0;

        NatsMessage msg = new NatsMessage(subject, replyTo, null, null);

        assertEquals(msg.getProtocolBytes().length + 4, msg.getSizeInBytes(), "Size is set, with CRLF");
        assertEquals(protocol.getBytes(StandardCharsets.US_ASCII).length + 4, msg.getSizeInBytes(), "Size is correct");

        msg = new NatsMessage(subject, replyTo, null, null);

        assertEquals(msg.getProtocolBytes().length + 4, msg.getSizeInBytes(), "Size is set, with CRLF");
        assertEquals(protocol.getBytes(StandardCharsets.UTF_8).length + 4, msg.getSizeInBytes(), "Size is correct");
    }

    @Test
    public void testCustomMaxControlLine() {
        assertThrows(IllegalArgumentException.class, () -> {
            byte[] body = new byte[10];
            String subject = "subject";
            int maxControlLine = 1024;

            while (subject.length() <= maxControlLine) {
                subject += subject;
            }

            try (NatsTestServer ts = new NatsTestServer()) {
                Options options = new Options.Builder().
                        server(ts.getURI()).
                        maxReconnects(0).
                        maxControlLine(maxControlLine).
                        build();
                Connection nc = Nats.connect(options);
                standardConnectionWait(nc);
                nc.request(subject, body);
            }
        });
    }

    @Test
    public void testBigProtocolLineWithoutBody() {
        assertThrows(IllegalArgumentException.class, () -> {
            String subject = "subject";

            while (subject.length() <= Options.DEFAULT_MAX_CONTROL_LINE) {
                subject += subject;
            }

            try (NatsServerProtocolMock ts = new NatsServerProtocolMock(ExitAt.NO_EXIT);
                 NatsConnection nc = (NatsConnection) Nats.connect(ts.getURI())) {
                standardConnectionWait(nc);
                nc.subscribe(subject);
            }
        });
    }

    @Test
    public void testBigProtocolLineWithBody() {
        assertThrows(IllegalArgumentException.class, () -> {
            byte[] body = new byte[10];
            String subject = "subject";
            String replyTo = "reply";

            while (subject.length() <= Options.DEFAULT_MAX_CONTROL_LINE) {
                subject += subject;
            }

            try (NatsServerProtocolMock ts = new NatsServerProtocolMock(ExitAt.NO_EXIT);
                 NatsConnection nc = (NatsConnection) Nats.connect(ts.getURI())) {
                standardConnectionWait(nc);
                nc.publish(subject, replyTo, body);
            }
        });
    }


    @Test
    public void notJetStream() throws Exception {
        NatsMessage m = testMessage();
        m.ack();
        m.ackSync(Duration.ZERO);
        m.nak();
        m.nakWithDelay(Duration.ZERO);
        m.nakWithDelay(0);
        m.inProgress();
        m.term();
        assertThrows(IllegalStateException.class, m::metaData);
    }

    @Test
    public void miscCoverage() {
        NatsMessage m = NatsMessage.builder()
                .subject("test").replyTo("reply").utf8mode(true)
                .data("data", StandardCharsets.US_ASCII)
                .build();
        assertFalse(m.hasHeaders());
        assertFalse(m.isJetStream());
        assertFalse(m.isStatusMessage());
        assertNotNull(m.toString());
        assertNotNull(m.toDetailString());

        m = NatsMessage.builder()
            .subject("test").replyTo("reply")
            .data("very long data to string truncates with dot dot dot", StandardCharsets.US_ASCII)
            .build();
        assertNotNull(m.toString());
        assertTrue(m.toString().contains("..."));

        // no reply to, no data
        m = NatsMessage.builder().subject("test").build();
        assertNotNull(m.toString());
        assertNotNull(m.toDetailString());

        // no reply to, no empty data
        m = NatsMessage.builder().subject("test").data(new byte[0]).build();
        assertNotNull(m.toString());
        assertNotNull(m.toDetailString());

        // no reply to, no empty data
        m = NatsMessage.builder().subject("test").data((byte[])null).build();
        assertNotNull(m.toString());
        assertNotNull(m.toDetailString());

        // no reply to, no empty data
        m = NatsMessage.builder().subject("test").data((String)null).build();
        assertNotNull(m.toString());
        assertNotNull(m.toDetailString());

        List<String> data = dataAsLines("utf8-test-strings.txt");
        for (String d : data) {
            Message m1 = NatsMessage.builder().subject("test").data(d).build();
            Message m2 = NatsMessage.builder().subject("test").data(d, StandardCharsets.UTF_8).build();
            assertByteArraysEqual(m1.getData(), m2.getData());
        }

        m = testMessage();
        assertTrue(m.hasHeaders());
        assertNotNull(m.getHeaders());
        assertTrue(m.isUtf8mode());
        assertFalse(m.getHeaders().isEmpty());
        assertNull(m.getSubscription());
        assertNull(m.getNatsSubscription());
        assertNull(m.getConnection());
        assertEquals(23, m.getControlLineLength());
        assertNotNull(m.toDetailString()); // COVERAGE
        assertNotNull(m.getOrCreateHeaders());

        m.getHeaders().remove("key");
        assertFalse(m.hasHeaders());
        assertNotNull(m.getHeaders());

        m.headers = null; // we can do this because we have package access
        m.dirty = true; // for later tests, also is true b/c we nerfed the headers
        assertFalse(m.hasHeaders());
        assertNull(m.getHeaders());
        assertNotNull(m.toString()); // COVERAGE
        assertNotNull(m.getOrCreateHeaders());

        NatsMessage.ProtocolMessage pm = new NatsMessage.ProtocolMessage((byte[])null);
        assertNotNull(pm.protocolBab);
        assertEquals(0, pm.protocolBab.length());

        NatsMessage.InternalMessage scm = new NatsMessage.InternalMessage() {};
        assertNull(scm.protocolBab);
        assertEquals(-1, scm.getControlLineLength());

        // coverage coverage coverage
        NatsMessage nmCov = new NatsMessage("sub", "reply", null, true);
        assertTrue(nmCov.isUtf8mode());

        nmCov.dirty = false;
        nmCov.calculateIfDirty();

        nmCov.dirty = false;
        nmCov.headers = new Headers().add("foo", "bar");
        nmCov.calculateIfDirty();

        nmCov.dirty = false;
        nmCov.headers = new Headers().add("foo", "bar");
        nmCov.headers.getSerialized();
        nmCov.calculateIfDirty();

        assertTrue(nmCov.toDetailString().contains("HPUB sub reply 21 21"));
        assertTrue(nmCov.toDetailString().contains("next=No"));

        nmCov.protocolBab = null;
        nmCov.next = nmCov;
        assertTrue(nmCov.toDetailString().contains("protocolBytes=null"));
        assertTrue(nmCov.toDetailString().contains("next=Yes"));
    }

    @Test
    public void constructorWithMessage() {
        NatsMessage m = testMessage();

        NatsMessage copy = new NatsMessage(m);
        assertEquals(m.getSubject(), copy.getSubject());
        assertEquals(m.getReplyTo(), copy.getReplyTo());
        assertEquals(m.getData(), copy.getData());
        assertEquals(m.getSubject(), copy.getSubject());
        assertEquals(m.getSubject(), copy.getSubject());
    }

    @Test
    public void testFactoryProducesStatusMessage() {
        IncomingHeadersProcessor incomingHeadersProcessor =
                new IncomingHeadersProcessor("NATS/1.0 503 No Responders\r\n".getBytes());
        NatsMessage.InternalMessageFactory factory =
                new NatsMessage.InternalMessageFactory("sid", "subj", "replyTo", 0, false);
        factory.setHeaders(incomingHeadersProcessor);
        factory.setData(null); // coverage

        Message m = factory.getMessage();
        assertTrue(m.isStatusMessage());
        assertNotNull(m.getStatus());
        assertEquals(503, m.getStatus().getCode());
        assertNotNull(m.getStatus().toString());
        NatsMessage.StatusMessage sm = (NatsMessage.StatusMessage)m;
        assertNotNull(sm.toString());
    }

    private NatsMessage testMessage() {
        Headers h = new Headers();
        h.add("key", "value");

        return NatsMessage.builder()
                .subject("test").replyTo("reply").headers(h).utf8mode(true)
                .data("data", StandardCharsets.US_ASCII)
                .build();
    }
}