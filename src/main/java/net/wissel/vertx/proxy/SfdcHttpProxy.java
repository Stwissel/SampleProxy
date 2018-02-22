/********************************************************************************
 * Copyright (c) 2016, 2017 Julien Viet - https://github.com/vietj
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package net.wissel.vertx.proxy;

import java.util.function.Function;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import net.wissel.vertx.proxy.impl.SfdcHttpProxyImpl;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface SfdcHttpProxy extends Handler<HttpServerRequest> {

	static SfdcHttpProxy reverseProxy(HttpClient client) {
		return new SfdcHttpProxyImpl(client);
	}

	@Fluent
	SfdcHttpProxy filterSelector(Function<HttpClientResponse, ProxyFilter> filterSelector);

	/**
	 * Based on content of the response, mainly header or URI values a different
	 * content filter can be used
	 *
	 * @param response
	 *            from the request to server
	 * @return a function to transform the ReadStream
	 *
	 */
	ProxyFilter getResponseFilter(HttpClientResponse response);

	@Override
	void handle(HttpServerRequest request);

	ProxyRequest proxy(HttpServerRequest request, SocketAddress target);

	@Fluent
	SfdcHttpProxy selector(Function<HttpServerRequest, Future<SocketAddress>> selector);

	@Fluent
	SfdcHttpProxy target(int port, String host);

	@Fluent
	SfdcHttpProxy target(SocketAddress address);

}
