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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ParseTesting {
    static String infoJson = "{" + "\"server_id\":\"myserver\"" + "," + "\"version\":\"1.1.1\"" + ","
            + "\"go\": \"go1.9\"" + "," + "\"host\": \"host\"" + "," + "\"tls_required\": true" + ","
            + "\"auth_required\":false" + "," + "\"port\": 7777" + "," + "\"max_payload\":100000000000" + ","
            + "\"connect_urls\":[\"one\", \"two\"]" + "}";

    public static void main(String args[]) throws InterruptedException {
        int iterations = 10_000_000;

        System.out.println("###");
        System.out.printf("### Running parse tests with %s msgs.\n", NumberFormat.getInstance().format(iterations));
        System.out.println("###");

        runTest(iterations, "+OK");
        runTest(iterations, "PONG");
        runTest(iterations, "INFO " + infoJson);
        runTest(iterations, "MSG subject 22 replyto 234");
        runTest(iterations, "-ERR some error with spaces in it");

    }

    public static String[] splitCharBuffer(CharBuffer buffer) {
        ArrayList<String> list = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        while (buffer.hasRemaining()) {
            char c = buffer.get();

            if (c == ' ') {
                list.add(builder.toString());
                builder = new StringBuilder();
            } else {
                builder.append(c);
            }
        }

        if (builder.length() > 0) {
            list.add(builder.toString());
        }

        return list.toArray(new String[0]);
    }

    public static String grabNextWithBuilder(CharBuffer buffer) {
        StringBuilder builder = new StringBuilder();

        while (buffer.hasRemaining()) {
            char c = buffer.get();

            if (c == ' ') {
                return builder.toString();
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    public static String grabNext(CharBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return null;
        }

        int start = buffer.position();

        while (buffer.hasRemaining()) {
            char c = buffer.get();

            if (c == ' ') {
                int end = buffer.position();
                buffer.position(start);
                CharBuffer slice = buffer.subSequence(0, end-start-1); //don't grab the space
                buffer.position(end);
                return slice.toString();
            }
        }

        buffer.position(start);
        String retVal = buffer.toString();
        buffer.position(buffer.limit());
        return retVal;
    }

    public static String grabTheRest(CharBuffer buffer) {
        return buffer.toString();
    }

    public static void runTest(int iterations, String serverMessage) {
        Pattern space = Pattern.compile(" ");
        ByteBuffer protocolBuffer = ByteBuffer.allocate(32 * 1024);
        protocolBuffer.put(serverMessage.getBytes(StandardCharsets.UTF_8));
        protocolBuffer.flip();

        CharBuffer buffer = StandardCharsets.UTF_8.decode(protocolBuffer);
        String pl = buffer.toString();
        buffer.rewind();
        protocolBuffer.rewind();

        String[] newversion = splitCharBuffer(buffer);
        String[] oldversion = space.split(pl);

        buffer.rewind();
        ArrayList<String> protoAware = new ArrayList<>();
        String s = null;
        while((s = grabNext(buffer)) != null) {
            protoAware.add(s);
        }
        String[] protoAwareArray = protoAware.toArray(new String[0]);

        System.out.printf("### Parsing server string: %s\n", serverMessage);

        boolean newOk = Arrays.equals(newversion, oldversion);
        System.out.println("### Old and new versions are equal: " + newOk);

        if (!newOk) {
            System.exit(-1);
        }

        boolean protoOk = Arrays.equals(protoAwareArray, oldversion);
        System.out.println("### Old and protoaware versions are equal: " + protoOk);

        if (!protoOk) {
            System.exit(-1);
        }

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String protocolLine = StandardCharsets.UTF_8.decode(protocolBuffer).toString();
            space.split(protocolLine);
            protocolBuffer.rewind();
        }
        long end = System.nanoTime();

        System.out.printf("### %s old parses/sec.\n",
                NumberFormat.getInstance().format(1_000_000_000L * iterations / (end - start)));

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(protocolBuffer);
            splitCharBuffer(charBuffer);
            protocolBuffer.rewind();
        }
        end = System.nanoTime();

        System.out.printf("### %s new parses/sec.\n",
                NumberFormat.getInstance().format(1_000_000_000L * iterations / (end - start)));

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(protocolBuffer);
            String op = grabNext(charBuffer);

            switch (op) {
            case NatsConnection.OP_MSG:
                grabNext(charBuffer); //subject
                grabNext(charBuffer); // sid
                String replyTo = grabNext(charBuffer);
                String lengthString = grabNext(charBuffer);

                if (lengthString == null) {
                    lengthString = replyTo;
                    replyTo = null;
                }
                break;
            case NatsConnection.OP_ERR:
                grabTheRest(charBuffer);
                break;
            case NatsConnection.OP_OK:
            case NatsConnection.OP_PING:
            case NatsConnection.OP_PONG:
            case NatsConnection.OP_INFO:
            default:
                break;
            }
            protocolBuffer.rewind();
        }
        end = System.nanoTime();

        System.out.printf("### %s op-aware parses/sec.\n",
                NumberFormat.getInstance().format(1_000_000_000L * iterations / (end - start)));
        System.out.println();
    }
}