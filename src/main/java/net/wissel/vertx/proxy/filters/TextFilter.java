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
package net.wissel.vertx.proxy.filters;

import java.util.Collection;

import io.vertx.core.buffer.Buffer;

/**
 * @author SINLOANER8
 *
 */
public class TextFilter extends AbstractFilter {

	public TextFilter(boolean isChunked) {
		super(isChunked);
	}

	/* (non-Javadoc)
	 * @see net.wissel.salesforce.proxy.filters.AbstractFilter#filterBuffer(io.vertx.core.buffer.Buffer)
	 */
	// Little nonsense filter turning everything into uppercase
	@Override
	protected Buffer filterBuffer(Buffer incomingBuffer) {
		return Buffer.buffer(incomingBuffer.toString().toUpperCase());
	}

	@Override
	protected void filterEnd() {
		// No action required
	}

    @Override
    public void addSubfilters(Collection<String> subfilters) {
        // TODO Auto-generated method stub
        
    }
}
