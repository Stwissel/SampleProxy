/********************************************************************************
 * Copyright (c) 2016, 2017 Julien Viet - https://github.com/vietj
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package net.wissel.vertx.proxy.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import net.wissel.vertx.proxy.HttpRequestResponse;
import net.wissel.vertx.proxy.ProxyFilter;
import net.wissel.vertx.proxy.ProxyRequest;
import net.wissel.vertx.proxy.ProxyResponse;
import net.wissel.vertx.proxy.SfdcHttpProxy;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ProxyRequestImpl implements ProxyRequest {

    private class ProxyResponseImpl implements ProxyResponse {

        private final SfdcHttpProxy proxy;
        private HttpClientResponse  backResponse;
        private long                maxAge;
        private String              etag;
        private boolean             publicCacheControl;
        private boolean             sent;

        public ProxyResponseImpl(final SfdcHttpProxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public void cancel() {
            checkSent();
            sent = true;
            frontResponse.headers().clear();
            frontResponse.endHandler(null);
            backResponse.resume();
        }

        @Override
        public String etag() {
            return etag;
        }

        @Override
        public MultiMap headers() {
            return frontResponse.headers();
        }

        @Override
        public long maxAge() {
            return maxAge;
        }

        @Override
        public boolean publicCacheControl() {
            return publicCacheControl;
        }

        @Override
        public void send(Handler<AsyncResult<Void>> completionHandler) {
            checkSent();
            sent = true;

            // Determine chunked
            boolean chunkedCandidate = false;
            for (String value : backResponse.headers().getAll("transfer-encoding")) {
                if (value.equals("chunked")) {
                    chunkedCandidate = true;
                    break;
                } else {
                    frontRequest = null;
                    frontResponse.setStatusCode(501).end();
                    completionHandler.handle(Future.succeededFuture());
                    return;
                }
            }
            final boolean chunked = chunkedCandidate;
            
            frontResponse.exceptionHandler(err -> {
                HttpServerRequest request = stop();
                if (request != null) {
                    backRequest.reset();
                    completionHandler.handle(Future.failedFuture(err));
                }
            });

            backResponse.exceptionHandler(err -> {
                HttpServerRequest request = stop();
                if (request != null) {
                    request.response().close(); // Should reset instead ????
                    completionHandler.handle(Future.failedFuture(err));
                }
            });

            // Apply body filter based on the type of response
            HttpRequestResponse hrr = new HttpRequestResponse(backResponse, backRequest);
            ProxyFilter filter = proxy.getResponseFilter(hrr);
            Future<ReadStream<Buffer>> bodyStreamFuture = filter.apply(backResponse);

            bodyStreamFuture.setHandler(handler -> {
                if (handler.succeeded()) {
                    ReadStream<Buffer> bodyStream = handler.result();
                    if (frontRequest.method() == HttpMethod.HEAD) {
                        frontRequest = null;
                        frontResponse.end();
                        completionHandler.handle(Future.succeededFuture());
                    } else {
                        if (chunked && (frontRequest.version() == HttpVersion.HTTP_1_1)) {
                            frontResponse.setChunked(true);
                            responsePump = Pump.pump(bodyStream, frontResponse);
                            responsePump.start();
                            bodyStream.endHandler(v -> {
                                System.out.println(frontResponse.bytesWritten());
                                Future<Void> endgame = filter.end(frontResponse);
                                endgame.setHandler(h -> {
                                    frontRequest = null;
                                    frontResponse.end();
                                    completionHandler.handle(Future.succeededFuture());
                                });    
                            });
                        } else {
                            String contentLength = backResponse.getHeader("content-length");
                            if (contentLength != null) {
                                responsePump = Pump.pump(bodyStream, frontResponse);
                                responsePump.start();
                                bodyStream.endHandler(v -> {

                                    frontRequest = null;
                                    frontResponse.end();
                                    completionHandler.handle(Future.succeededFuture());
                                });
                            } else {
                                Buffer body = Buffer.buffer();
                                bodyStream.handler(body::appendBuffer);
                                bodyStream.endHandler(v -> {
                                    frontRequest = null;
                                    frontResponse.end(body);
                                    completionHandler.handle(Future.succeededFuture());
                                });
                            }
                        }
                    }
                }
                backResponse.resume();
            });
        }

        @Override
        public ProxyResponse set(HttpClientResponse backResponse) {
            checkSent();

            frontResponse.headers().clear();
            this.backResponse = backResponse;

            long maxAge = -1;
            boolean publicCacheControl = false;
            String cacheControlHeader = backResponse.getHeader(HttpHeaders.CACHE_CONTROL);
            if (cacheControlHeader != null) {
                CacheControl cacheControl = new CacheControl().parse(cacheControlHeader);
                if (cacheControl.isPublic()) {
                    publicCacheControl = true;
                    if (cacheControl.maxAge() > 0) {
                        maxAge = (long) cacheControl.maxAge() * 1000;
                    } else {
                        String dateHeader = backResponse.getHeader(HttpHeaders.DATE);
                        String expiresHeader = backResponse.getHeader(HttpHeaders.EXPIRES);
                        if ((dateHeader != null) && (expiresHeader != null)) {
                            maxAge = ParseUtils.parseHeaderDate(expiresHeader).getTime()
                                    - ParseUtils.parseHeaderDate(dateHeader).getTime();
                        }
                    }
                }
            }
            this.maxAge = maxAge;
            this.publicCacheControl = publicCacheControl;
            etag = backResponse.getHeader(HttpHeaders.ETAG);

            frontResponse.setStatusCode(backResponse.statusCode());
            frontResponse.setStatusMessage(backResponse.statusMessage());

            // Date header
            String dateHeader = backResponse.headers().get("date");
            Date date = null;
            if (dateHeader == null) {
                List<String> warningHeaders = backResponse.headers().getAll("warning");
                if (warningHeaders.size() > 0) {
                    for (String warningHeader : warningHeaders) {
                        date = ParseUtils.parseWarningHeaderDate(warningHeader);
                        if (date != null) {
                            break;
                        }
                    }
                }
            } else {
                date = ParseUtils.parseHeaderDate(dateHeader);
            }
            if (date == null) {
                date = new Date();
            }
            try {
                frontResponse.putHeader("date", ParseUtils.formatHttpDate(date));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Suppress incorrect warning header
            List<String> warningHeaders = backResponse.headers().getAll("warning");
            if (warningHeaders.size() > 0) {
                warningHeaders = new ArrayList<>(warningHeaders);
                Date dateInstant = ParseUtils.parseHeaderDate(dateHeader);
                Iterator<String> i = warningHeaders.iterator();
                while (i.hasNext()) {
                    String warningHeader = i.next();
                    Date warningInstant = ParseUtils.parseWarningHeaderDate(warningHeader);
                    if ((warningInstant != null) && (dateInstant != null) && !warningInstant.equals(dateInstant)) {
                        i.remove();
                    }
                }
            }
            frontResponse.putHeader("warning", warningHeaders);

            // Handle other headers
            backResponse.headers().forEach(header -> {
                String name = header.getKey();
                String value = header.getValue();
                if (name.equalsIgnoreCase("date") || name.equalsIgnoreCase("warning")
                        || name.equalsIgnoreCase("transfer-encoding")) {
                    // Skip
                } else {
                    frontResponse.headers().add(name, value);
                }
            });

            return this;
        }

        @Override
        public int statusCode() {
            return frontResponse.getStatusCode();
        }

        @Override
        public String statusMessage() {
            return frontResponse.getStatusMessage();
        }

        private void checkSent() {
            if (sent) {
                throw new IllegalStateException();
            }
        }
    }

    private final SfdcHttpProxy proxy;
    private HttpServerRequest   frontRequest;

    private HttpServerResponse frontResponse;

    private Function<HttpServerRequest, HttpClientRequest> provider;
    // private Function<ReadStream<Buffer>, ReadStream<Buffer>> bodyFilter =
    // Function.identity();

    private MultiMap          headers;
    private HttpClientRequest backRequest;
    private Pump              requestPump;

    private Pump responsePump;

    public ProxyRequestImpl(final SfdcHttpProxy proxy, final Function<HttpServerRequest, HttpClientRequest> provider,
            final HttpServerRequest request) {
        if (request == null) {
            throw new NullPointerException();
        }
        this.provider = provider;
        frontRequest = request;
        this.proxy = proxy;
    }

    public ProxyRequestImpl(final SfdcHttpProxy proxy, final HttpClient client, final SocketAddress target,
            final HttpServerRequest request) {
        this(proxy, req -> {
            HttpMethod method = req.method();
            HttpClientRequest backRequest = client.request(method, target.port(), target.host(), req.uri());
            if (method == HttpMethod.OTHER) {
                backRequest.setRawMethod(req.rawMethod());
            }
            return backRequest;
        }, request);
    }

    @Override
    public MultiMap headers() {
        if (headers == null) {
            headers = MultiMap.caseInsensitiveMultiMap();
            copyHeaders(headers);
        }
        return headers;
    }

    @Override
    public void proxy(Handler<AsyncResult<Void>> completionHandler) {
        send(ar -> {
            if (ar.succeeded()) {
                ProxyResponse resp = ar.result();
                resp.send(completionHandler);
            } else {
                completionHandler.handle(ar.mapEmpty());
            }
        });
    }

    @Override
    public void send(Handler<AsyncResult<ProxyResponse>> completionHandler) {

        // Sanity check 1
        try {
            frontRequest.version();
        } catch (IllegalStateException e) {
            // Sends 501
            frontRequest.resume();
            completionHandler.handle(Future.failedFuture(e));
            return;
        }

        // Create back request
        backRequest = provider.apply(frontRequest);

        // Encoding check
        List<String> te = frontRequest.headers().getAll("transfer-encoding");
        if (te != null) {
            for (String val : te) {
                if (val.equals("chunked")) {
                    backRequest.setChunked(true);
                } else {
                    frontRequest.resume().response().setStatusCode(400).end();
                    // I think we should make a call to completion handler at
                    // this point - it does
                    // not seem to be tested
                    return;
                }
            }
        }

        //
        backRequest.handler(resp -> handle(resp, completionHandler));

        // Set headers
        if (headers != null) {
            // Handler specially the host header
            String host = headers.get("host");
            if (host != null) {
                headers.remove("host");
                backRequest.setHost(host);
            }
            backRequest.headers().setAll(headers);
        } else {
            copyHeaders(backRequest.headers());
        }

        // No application for Body filter for outgoing requests
        Function<ReadStream<Buffer>, ReadStream<Buffer>> frontBodyFunction = Function.identity();
        ReadStream<Buffer> bodyStream = frontBodyFunction.apply(frontRequest);

        bodyStream.endHandler(v -> {
            requestPump = null;
            backRequest.end();
            if (frontResponse == null) {
                frontRequest.response().exceptionHandler(err -> {
                    if (stop() != null) {
                        backRequest.reset();
                        completionHandler.handle(Future.failedFuture(err));
                    }
                });
            }
        });
        requestPump = Pump.pump(bodyStream, backRequest);
        backRequest.exceptionHandler(err -> {
            if (resetClient()) {
                completionHandler.handle(Future.failedFuture(err));
            }
        });
        frontRequest.exceptionHandler(err -> {
            if (stop() != null) {
                backRequest.reset();
                completionHandler.handle(Future.failedFuture(err));
            }
        });

        requestPump.start();
        bodyStream.resume();
    }

    private void copyHeaders(MultiMap to) {
        // Set headers, don't copy host, as HttpClient will set it
        for (Map.Entry<String, String> header : frontRequest.headers()) {
            if (header.getKey().equalsIgnoreCase("host")) {
                //
            } else {
                to.add(header.getKey(), header.getValue());
            }
        }
    }

    private void handle(HttpClientResponse backResponse, Handler<AsyncResult<ProxyResponse>> completionHandler) {
        if (frontRequest == null) {
            return;
        }
        backResponse.pause();
        frontResponse = frontRequest.response();
        frontResponse.exceptionHandler(null); // Might have been set previously
        ProxyResponseImpl response = new ProxyResponseImpl(proxy);
        response.set(backResponse);
        completionHandler.handle(Future.succeededFuture(response));
    }

    private boolean resetClient() {
        HttpServerRequest request = stop();
        if (request != null) {
            HttpConnection conn = request.connection();
            HttpServerResponse response = request.response();
            response.setStatusCode(502).end();
            if (conn != null) {
                conn.close();
            }
            return true;
        }
        return false;
    }

    /**
     * Stop the proxy request
     *
     * @return the front request if stopped / {@code null} means nothing
     *         happened
     */
    private HttpServerRequest stop() {
        HttpServerRequest request = frontRequest;
        if (request != null) {
            // Abrupt close
            frontRequest = null;
            if (requestPump != null) {
                requestPump.stop();
                requestPump = null;
            }
            if (responsePump != null) {
                responsePump.stop();
                responsePump = null;
            }
            return request;
        }
        return null;
    }

}
