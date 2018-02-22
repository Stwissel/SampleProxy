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

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientResponse;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface ProxyResponse {

	/**
	 * Cancels the proxy request, this will release the resources and clear the
	 * headers of the wrapped {@link io.vertx.core.http.HttpServerResponse}.
	 */
	void cancel();

	String etag();

	/**
	 * @return the headers that will be sent to the client, the returned headers can
	 *         be modified
	 */
	MultiMap headers();

	long maxAge();

	boolean publicCacheControl();

	/**
	 * Send the proxy response to the client.
	 *
	 * @param completionHandler
	 *            the handler to be called when the response has been sent
	 */
	void send(Handler<AsyncResult<Void>> completionHandler);

	// @Fluent
	// ProxyResponse bodyFilter(Function<ReadStream<Buffer>, ReadStream<Buffer>>
	// filter);

	/**
	 * Set the proxy response to use the {@code response}, this will update the
	 * values returned by {@link #statusCode()}, {@link #statusMessage()},
	 * {@link #headers()}, {@link #publicCacheControl()}, {@link #maxAge()}.
	 *
	 * @param response
	 *            the response to use
	 */
	@Fluent
	ProxyResponse set(HttpClientResponse response);

	int statusCode();

	String statusMessage();

}
