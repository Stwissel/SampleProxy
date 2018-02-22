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

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.wissel.vertx.proxy.ProxyFilter;

/**
 * Simplified base class for filter implementation
 * 
 * @author stw
 *
 */
public abstract class AbstractFilter implements ProxyFilter {

    protected final boolean isChunked;
    private Buffer          internalBuffer = null;

    protected void appendInternalBuffer(Buffer buffer) {
        if (this.internalBuffer == null) {
            this.internalBuffer = Buffer.buffer(buffer.getByteBuf());
        } else {
            this.internalBuffer.appendBuffer(buffer);
        }
    }

    public AbstractFilter(final boolean isChunked) {
        this.isChunked = isChunked;
    }

    @Override
    public ReadStream<Buffer> apply(final ReadStream<Buffer> raw) {
        if (raw == null) {
            return null;
        }

        ReadStream<Buffer> result = new ReadStream<Buffer>() {

            @Override
            public ReadStream<Buffer> endHandler(Handler<Void> handler) {
                raw.endHandler(handler);
                return this;
            }

            @Override
            public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
                raw.exceptionHandler(handler);
                return this;
            }

            @Override
            public ReadStream<Buffer> handler(Handler<Buffer> handler) {
                Handler<Buffer> innerHandler = new Handler<Buffer>() {

                    @Override
                    public void handle(Buffer buffer) {
                        final Buffer innerBuffer = AbstractFilter.this.filterBuffer(buffer);
                        if (innerBuffer.length() > 0) {
                            handler.handle(innerBuffer);
                        }
                    }
                };
                raw.handler(innerHandler);
                return this;
            }

            @Override
            public ReadStream<Buffer> pause() {
                raw.pause();
                return this;
            }

            @Override
            public ReadStream<Buffer> resume() {
                raw.resume();
                return this;
            }
        };

        return result;
    }

    /**
     * 
     * @param incomingBuffer
     *            raw values from source
     * @return a transformed buffer with new values
     */
    protected abstract Buffer filterBuffer(Buffer incomingBuffer);

    @Override
    public void end(WriteStream<Buffer> result) {
        this.filterEnd();
        if (this.internalBuffer != null) {
            result.write(internalBuffer);
        }
    }

    /**
     * Function to call before internalBuffer is written to the result - needed
     * to deal with junked results
     */
    protected abstract void filterEnd();

}
