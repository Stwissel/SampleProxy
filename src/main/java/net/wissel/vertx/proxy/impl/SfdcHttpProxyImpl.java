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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.core.streams.ReadStream;
import net.wissel.vertx.proxy.ProxyFilter;
import net.wissel.vertx.proxy.ProxyRequest;
import net.wissel.vertx.proxy.ProxyResponse;
import net.wissel.vertx.proxy.SfdcHttpProxy;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SfdcHttpProxyImpl implements SfdcHttpProxy {

	private class Resource implements Function<ReadStream<Buffer>, ReadStream<Buffer>> {

		private final String absoluteUri;
		private final int statusCode;
		private final String statusMessage;
		private final MultiMap headers;
		private final long timestamp;
		private final long maxAge;
		private final Date lastModified;
		private final String etag;
		private Buffer content = Buffer.buffer();

		public Resource(String absoluteUri, int statusCode, String statusMessage, MultiMap headers, long timestamp,
				long maxAge) {
			String lastModifiedHeader = headers.get(HttpHeaders.LAST_MODIFIED);
			this.absoluteUri = absoluteUri;
			this.statusCode = statusCode;
			this.statusMessage = statusMessage;
			this.headers = headers;
			this.timestamp = timestamp;
			this.maxAge = maxAge;
			lastModified = lastModifiedHeader != null ? ParseUtils.parseHeaderDate(lastModifiedHeader) : null;
			etag = headers.get(HttpHeaders.ETAG);
		}

		@Override
		public ReadStream<Buffer> apply(ReadStream<Buffer> s) {
			return new ReadStream<Buffer>() {
				@Override
				public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
					if (endHandler != null) {
						s.endHandler(v -> {
							cache.put(absoluteUri, Resource.this);
							endHandler.handle(null);
						});
					} else {
						s.endHandler(null);
					}
					return this;
				}

				@Override
				public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
					s.exceptionHandler(handler);
					return this;
				}

				@Override
				public ReadStream<Buffer> handler(Handler<Buffer> handler) {
					if (handler != null) {
						s.handler(buff -> {
							content.appendBuffer(buff);
							handler.handle(buff);
						});
					} else {
						s.handler(null);
					}
					return this;
				}

				@Override
				public ReadStream<Buffer> pause() {
					s.pause();
					return this;
				}

				@Override
				public ReadStream<Buffer> resume() {
					s.resume();
					return this;
				}
			};
		}

		HttpClientRequest request() {
			return new HttpClientRequest() {
				Handler<HttpClientResponse> responseHandler;

				@Override
				public String absoluteURI() {
					return null;
				}

				@Override
				public HttpConnection connection() {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest connectionHandler(Handler<HttpConnection> handler) {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest continueHandler(Handler<Void> handler) {
					return this;
				}

				@Override
				public HttpClientRequest drainHandler(Handler<Void> handler) {
					return this;
				}

				@Override
				public void end() {
					CachedHttpClientResponse resp = response();
					responseHandler.handle(resp);
					// Need a tick
					Vertx.currentContext().runOnContext(v -> {
						resp.send();
					});
				}

				@Override
				public void end(Buffer chunk) {
					end();
				}

				@Override
				public void end(String chunk) {
					end();
				}

				@Override
				public void end(String chunk, String enc) {
					end();
				}

				@Override
				public HttpClientRequest endHandler(Handler<Void> endHandler) {
					return this;
				}

				@Override
				public HttpClientRequest exceptionHandler(Handler<Throwable> handler) {
					return this;
				}

				@Override
				public String getHost() {
					throw new UnsupportedOperationException();
				}

				@Override
				public String getRawMethod() {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest handler(Handler<HttpClientResponse> handler) {
					responseHandler = handler;
					return this;
				}

				@Override
				public MultiMap headers() {
					return MultiMap.caseInsensitiveMultiMap();
				}

				@Override
				public boolean isChunked() {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpMethod method() {
					throw new UnsupportedOperationException();
				}

				@Override
				public String path() {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest pause() {
					return this;
				}

				@Override
				public HttpClientRequest pushHandler(Handler<HttpClientRequest> handler) {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest putHeader(CharSequence name, CharSequence value) {
					return this;
				}

				@Override
				public HttpClientRequest putHeader(CharSequence name, Iterable<CharSequence> values) {
					return this;
				}

				@Override
				public HttpClientRequest putHeader(String name, Iterable<String> values) {
					return this;
				}

				@Override
				public HttpClientRequest putHeader(String name, String value) {
					return this;
				}

				@Override
				public String query() {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean reset(long code) {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest resume() {
					return this;
				}

				@Override
				public HttpClientRequest sendHead() {
					return this;
				}

				@Override
				public HttpClientRequest sendHead(Handler<HttpVersion> completionHandler) {
					return this;
				}

				@Override
				public HttpClientRequest setChunked(boolean chunked) {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest setFollowRedirects(boolean followRedirects) {
					return this;
				}

				@Override
				public HttpClientRequest setHost(String host) {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest setRawMethod(String method) {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest setTimeout(long timeoutMs) {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest setWriteQueueMaxSize(int maxSize) {
					return this;
				}

				@Override
				public String uri() {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientRequest write(Buffer data) {
					return this;
				}

				@Override
				public HttpClientRequest write(String chunk) {
					return this;
				}

				@Override
				public HttpClientRequest write(String chunk, String enc) {
					return this;
				}

				@Override
				public HttpClientRequest writeCustomFrame(int type, int flags, Buffer payload) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean writeQueueFull() {
					return false;
				}
			};
		}

		CachedHttpClientResponse response() {
			return new CachedHttpClientResponse() {
				Handler<Buffer> dataHandler;
				Handler<Void> endHandler;

				@Override
				public HttpClientResponse bodyHandler(Handler<Buffer> bodyHandler) {
					throw new UnsupportedOperationException();
				}

				@Override
				public List<String> cookies() {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientResponse customFrameHandler(Handler<HttpFrame> handler) {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientResponse endHandler(Handler<Void> handler) {
					endHandler = handler;
					return this;
				}

				@Override
				public HttpClientResponse exceptionHandler(Handler<Throwable> handler) {
					return this;
				}

				@Override
				public String getHeader(CharSequence headerName) {
					return headers.get(headerName);
				}

				@Override
				public String getHeader(String headerName) {
					return headers.get(headerName);
				}

				@Override
				public String getTrailer(String trailerName) {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientResponse handler(Handler<Buffer> handler) {
					dataHandler = handler;
					return this;
				}

				@Override
				public MultiMap headers() {
					return headers;
				}

				@Override
				public NetSocket netSocket() {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientResponse pause() {
					return this;
				}

				@Override
				public HttpClientRequest request() {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpClientResponse resume() {
					return this;
				}

				@Override
				public void send() {
					if (dataHandler != null) {
						dataHandler.handle(content);
					}
					if (endHandler != null) {
						endHandler.handle(null);
					}
				}

				@Override
				public int statusCode() {
					return statusCode;
				}

				@Override
				public String statusMessage() {
					return statusMessage;
				}

				@Override
				public MultiMap trailers() {
					throw new UnsupportedOperationException();
				}

				@Override
				public HttpVersion version() {
					throw new UnsupportedOperationException();
				}
			};
		}

		boolean revalidate(ProxyResponse response) {
			if ((etag != null) && (response.etag() != null)) {
				boolean valid = etag.equals(response.etag());
				if (!valid) {
					cache.remove(absoluteUri);
				}
				return valid;
			}
			return true;
		}

	}

	private final HttpClient client;
	private Function<HttpServerRequest, Future<SocketAddress>> targetSelector = req -> Future
			.failedFuture("No target available");
	private final Map<String, Resource> cache = new HashMap<>();

	private Function<HttpClientResponse, ProxyFilter> localFilterSelector = null;

	public SfdcHttpProxyImpl(HttpClient client) {
		this.client = client;
	}

	@Override
	public SfdcHttpProxy filterSelector(Function<HttpClientResponse, ProxyFilter> filterSelector) {
		localFilterSelector = filterSelector;
		return this;
	}

	@Override
	public ProxyFilter getResponseFilter(HttpClientResponse response) {
		return localFilterSelector.apply(response);
	}

	@Override
	public void handle(HttpServerRequest request) {
		if (!serveFromCache(request)) {
			doReq(request, null);
		}
	}

	@Override
	public ProxyRequest proxy(HttpServerRequest request, SocketAddress target) {
		return new ProxyRequestImpl(this, client, target, request);
	}

	@Override
	public SfdcHttpProxy selector(Function<HttpServerRequest, Future<SocketAddress>> selector) {
		targetSelector = selector;
		return this;
	}

	@Override
	public SfdcHttpProxy target(int port, String host) {
		return target(new SocketAddressImpl(port, host));
	}

	@Override
	public SfdcHttpProxy target(SocketAddress address) {
		targetSelector = req -> Future.succeededFuture(address);
		return this;
	}

	private void doReq(HttpServerRequest request, Resource resource) {
		request.pause();
		Future<SocketAddress> fut = targetSelector.apply(request);
		fut.setHandler(ar -> {
			if (ar.succeeded()) {
				SocketAddress target = ar.result();
				ProxyRequestImpl proxyReq = new ProxyRequestImpl(this, client, target, request);

				if ((resource != null) && (resource.etag != null)) {
					proxyReq.headers().set(HttpHeaders.IF_NONE_MATCH, resource.etag);
				}

				proxyReq.send(ar1 -> {
					if (ar1.succeeded()) {
						ProxyResponse proxyResp = ar1.result();
						if ((resource != null)
								&& ((proxyResp.statusCode() == 200) || (proxyResp.statusCode() == 304))) {
							if (resource.revalidate(proxyResp)) {
								CachedHttpClientResponse cachedResp = resource.response();
								proxyResp.set(cachedResp);
								proxyResp.send(ar2 -> {
									// Done
								});
								cachedResp.send();
							} else {
								// Force a new request
								proxyResp.cancel();
								CachedHttpServerRequest req = new CachedHttpServerRequest(request);
								doReq(req, null); // Should we use something else than null ?
							}
							return;
						}

						if ((request.method() == HttpMethod.GET) && proxyResp.publicCacheControl()
								&& (proxyResp.maxAge() > 0)) {
							Resource res = new Resource(request.absoluteURI(), proxyResp.statusCode(),
									proxyResp.statusMessage(), proxyResp.headers(), System.currentTimeMillis(),
									proxyResp.maxAge());
							// TODO do we need a body filter here?
							// proxyResp.bodyFilter(res);
						}
						proxyResp.send(ar2 -> {
							// Done
						});
					}
				});
			} else {
				request.resume();
				request.response().setStatusCode(404).end();
			}
		});
	}

	private boolean serveFromCache(HttpServerRequest request) {
		String cacheKey = request.absoluteURI();
		Resource resource = cache.get(cacheKey);
		if (resource == null) {
			return false;
		}
		long now = System.currentTimeMillis();
		if ((resource.timestamp + resource.maxAge) < now) {
			cache.remove(cacheKey);
			return false;
		}
		String cacheControlHeader = request.getHeader(HttpHeaders.CACHE_CONTROL);
		if (cacheControlHeader != null) {
			CacheControl cacheControl = new CacheControl().parse(cacheControlHeader);
			if (cacheControl.maxAge() >= 0) {
				long currentAge = now - resource.timestamp;
				if (currentAge > (cacheControl.maxAge() * 1000)) {
					String etag = resource.headers.get(HttpHeaders.ETAG);
					if (etag != null) {
						doReq(request, resource);
						return true;
					} else {
						return false;
					}
				}
			}
		}

		String ifModifiedSinceHeader = request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
		if (((request.method() == HttpMethod.GET) || (request.method() == HttpMethod.HEAD))
				&& (ifModifiedSinceHeader != null) && (resource.lastModified != null)) {
			Date ifModifiedSince = ParseUtils.parseHeaderDate(ifModifiedSinceHeader);
			if (resource.lastModified.getTime() <= ifModifiedSince.getTime()) {
				request.response().setStatusCode(304).end();
				return true;
			}
		}

		ProxyRequestImpl proxyReq = new ProxyRequestImpl(this, req -> resource.request(), request);
		proxyReq.send(ar1 -> {
			ProxyResponse proxyResp = ar1.result();
			proxyResp.send(ar2 -> {
				// Done
			});
		});
		return true;
	}
}
