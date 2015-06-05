/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.service.http.proxy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

import javax.net.SocketFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class HttpProxyPersistenceTest {

    private static final int KEEP_ALIVE_TIMEOUT = 5;
    private static final int KEEP_ALIVE_MAX_CONNECTIONS = 1;

    @Rule
    public TestRule timeout = new DisableOnDebug(new Timeout(15, SECONDS));

    @Test
    public void maxPersistentIdleConnections() throws Exception {
        Gateway gateway = new Gateway();
        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .service()
                        .accept(URI.create("http://localhost:8110"))
                        .connect(URI.create("http://localhost:8080"))
                        .type("http.proxy")
                        .connectOption("http.keepalive.timeout", String.valueOf(KEEP_ALIVE_TIMEOUT))
                        .connectOption("http.keepalive.max.connections", String.valueOf(KEEP_ALIVE_MAX_CONNECTIONS))
                    .done()
                .done();
        // @formatter:on

        ServerHandler handler = new ServerHandler();
        OriginServer originServer = new OriginServer(8080, handler);

        try {
            originServer.start();
            gateway.start(configuration);

            // t1 client sends 2 requests
            Thread t1 = new Thread(new HttpClient());
            t1.start(); t1.join();
            // server should have received only one connection
            // pool should have max configured connections = 1
            assertEquals(1, handler.getConnections());

            // t2 client sends 2 requests
            Thread t2 = new Thread(new HttpClient());
            t2.start(); t2.join();

            // case 1: t2 client may pick up cached connection. In that case, gateway doesn't make
            // new connections. So max connections would be 1
            // case 2: t2 client may not pick up cached connection. In that case, gateway makes
            // 2 new connections for 2 requests(note that the pool is full). So the max connections
            // would be 3
            int max = handler.getConnections();
            if (max != 1 && max != 3) {
                throw new AssertionError("Expected 1 or 3 max no of connections to server, but got="+max);
            }
        } finally {
            gateway.stop();
            originServer.stop();
        }
    }

    private static class HttpClient implements Runnable {
        static final byte[] HTTP_REQUEST =
                ("GET / HTTP/1.1\r\n" +
                "Host: localhost:8110\r\n" +
                "Cache-Control: max-age=0\r\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n" +
                "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:8.0) Gecko/20100101 Firefox/8.0\r\n" +
                "Accept-Encoding: gzip, deflate, sdch\r\n" +
                "Accept-Language: en-US,en;q=0.8\r\n" +
                "\r\n").getBytes(UTF_8);

        @Override
        public void run() {
            try (Socket socket = SocketFactory.getDefault().createSocket("localhost", 8110);
                 InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                // 2 requests per connection
                for(int i=0; i < 2; i++) {
                    // read and write HTTP request and response headers
                    out.write(HTTP_REQUEST);
                    OriginServer.parseHttpHeaders(in);
                    readFully(in, new byte[31]);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }

    private static class ServerHandler implements OriginServer.Handler {

        static final byte[] HTTP_RESPONSE =
                ("HTTP/1.1 200 OK\r\n" +
                "Server: Apache-Coyote/1.1\r\n" +
                "Content-Type: text/html;charset=UTF-8\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "Date: Tue, 10 Feb 2015 02:17:15 GMT\r\n" +
                "\r\n" +
                "14\r\n" +
                "<html>Hellooo</html>\r\n" +
                "0\r\n" +
                "\r\n").getBytes(UTF_8);

        private int connections;

        @Override
        public void handle(Socket serverSocket) throws IOException {
            connections++;
            new Thread(() -> {
                try(Socket socket = serverSocket;
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream()) {

                    // read and write HTTP request and response headers
                    while(OriginServer.parseHttpHeaders(in)) {
                        out.write(HTTP_RESPONSE);
                        out.flush();
                    }
                } catch(Exception ioe) {
                    ioe.printStackTrace();
                }
            }).start();
        }

        int getConnections() {
            return connections;
        }

    }

    static void readFully(InputStream in, byte b[]) throws IOException {
        int n = 0;
        while (n < b.length) {
            int count = in.read(b, n, b.length - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        }
    }

}
