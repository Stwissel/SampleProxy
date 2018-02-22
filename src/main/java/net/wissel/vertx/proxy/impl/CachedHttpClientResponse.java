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

import io.vertx.core.http.HttpClientResponse;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface CachedHttpClientResponse extends HttpClientResponse {

  void send();

}
