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

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface ProxyRequest {

	/**
	 * @return the headers that will be sent to the target, the returned headers can
	 *         be modified
	 */
	MultiMap headers();

	// TODO: Replace with filter providing function
	// @Fluent
	// ProxyRequest bodyFilter(Function<ReadStream<Buffer>, ReadStream<Buffer>>
	// filter);

	void proxy(Handler<AsyncResult<Void>> completionHandler);

	void send(Handler<AsyncResult<ProxyResponse>> completionHandler);

}
