// Copyright 2020 The NATS Authors
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

package io.nats.examples.jetstream;

import io.nats.client.*;
import io.nats.client.api.SimpleConsumerConfiguration;
import io.nats.client.api.StorageType;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static io.nats.examples.jetstream.NatsJsUtils.createOrReplaceStream;

/**
 * This example will demonstrate JetStream2 and simplified listening
 */
public class NatsSimpleListenNewConsumer {
    static String stream = "simple-stream";
    static String subject = "simple-subject";
    static String durable = "simple-durable";
    static int count = 20;

    public static void main(String[] args) {

        try (Connection nc = Nats.connect("nats://localhost")) {

            setupStreamAndData(nc);

            // JetStream2 context
            JetStreamSimplified js = nc.jetStreamSimplified(stream);

            // ********************************************************************************
            // Messages are sent to the handler without having to do nextMessage
            // ********************************************************************************
            CountDownLatch latch = new CountDownLatch(count);
            AtomicInteger red = new AtomicInteger();
            MessageHandler handler = msg -> {
                System.out.printf("Subject: %s | Data: %s | Meta: %s\n",
                    msg.getSubject(), new String(msg.getData()), msg.getReplyTo());
                msg.ack();
                red.incrementAndGet();
                latch.countDown();
            };

            // ********************************************************************************
            // Listener is just a simple consumer. Messages are sent to the handler.
            // Keep a reference to the SimpleConsumer to be able to unsub, drain and get consumer info
            // ********************************************************************************
            SimpleConsumerOptions sco = SimpleConsumerOptions.builder()
                .batchSize(10)
                .repullAt(5)
//                .maxBytes(999)
//                .expiresIn(Duration.ofMillis(10000))
                .build();

            SimpleConsumerConfiguration scc = SimpleConsumerConfiguration
                .simpleBuilder(">")
                .durable(durable)
                .build();

            SimpleConsumer simpleConsumer = js.endlessListen(scc, handler, sco);
            // ********************************************************************************

            latch.await();

            System.out.println("\n" + red.get() + " message(s) were received.\n");
            System.out.println("\n" + simpleConsumer.getConsumerInfo());

            // be a good citizen
            simpleConsumer.unsubscribe();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupStreamAndData(Connection nc) throws IOException, JetStreamApiException {
        JetStreamManagement jsm = nc.jetStreamManagement();
        createOrReplaceStream(jsm, stream, StorageType.Memory, subject);

        JetStreamSimplified js = nc.jetStreamSimplified(stream);
        for (int x = 1; x <= count; x++) {
            js.getJetStream().publish(subject, ("m-" + x).getBytes());
        }
    }
}