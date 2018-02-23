/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <swissel@salesforce.com>              *
 *                                       @notessensei                         *
 * @version     1.0                                                           *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== *
 */
package net.wissel.vertx.proxy;

import java.util.Collection;
import java.util.function.Function;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 * @author SINLOANER8
 *
 */
public interface ProxyFilter extends Function<ReadStream<Buffer>, ReadStream<Buffer>> {
	
	/**
	 * Write (eventually) the result of a chunked operation
	 * @param result
	 */
	public void end(WriteStream<Buffer> result);

	/**
	 * Adds the name of specific subfilters that
	 * need to be processes, so content runs through
	 * a filter chain
	 * 
	 * @param subfilters Class name for sub filters
	 */
    public void addSubfilters(Collection<String> subfilters);

}
