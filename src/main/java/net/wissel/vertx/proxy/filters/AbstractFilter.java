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

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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

    protected final Logger  logger         = LoggerFactory.getLogger(this.getClass());
    private Buffer          chunkCollector = null;
    protected final boolean isChunked;
    private Buffer          internalBuffer = null;
    private final Vertx     vertx;

    public AbstractFilter(final Vertx vertx, final boolean isChunked) {
        this.isChunked = isChunked;
        this.vertx = vertx;
    }

    @Override
    public Future<ReadStream<Buffer>> apply(final ReadStream<Buffer> raw) {
        if (raw == null) {
            return null;
        }

        final ReadStream<Buffer> result = new ReadStream<Buffer>() {

            @Override
            public ReadStream<Buffer> endHandler(final Handler<Void> handler) {
                raw.endHandler(handler);
                return this;
            }

            @Override
            public ReadStream<Buffer> exceptionHandler(final Handler<Throwable> handler) {
                raw.exceptionHandler(handler);
                return this;
            }

            @Override
            public ReadStream<Buffer> handler(final Handler<Buffer> handler) {
                final Handler<Buffer> innerHandler = new Handler<Buffer>() {

                    @Override
                    public void handle(final Buffer buffer) {
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

        return Future.succeededFuture(result);
    }

    @Override
    public Future<Void> end(final WriteStream<Buffer> result) {
        Future<Void> endgame = Future.future();
        final Future<Buffer> endOperation = this.filterEnd();
        endOperation.setHandler(handler -> {
            if (handler.succeeded()) {
                Buffer b = handler.result();
                if (b.length() > 0) {
                    result.write(b);
                }
                endgame.complete();
            } else {
                endgame.fail(handler.cause());
            }         
        });
        return endgame;
    }

    /**
     * @return the vertx
     */
    public Vertx getVertx() {
        return vertx;
    }

    protected void appendInternalBuffer(final Buffer buffer) {
        if (this.internalBuffer == null) {
            this.internalBuffer = Buffer.buffer(buffer.getByteBuf());
        } else {
            this.internalBuffer.appendBuffer(buffer);
        }
    }

    /**
     *
     * @param incomingBuffer
     *            raw values from source
     * @return a transformed buffer with new values
     */
    protected Buffer filterBuffer(final Buffer incomingBuffer) {
        if (this.isChunked) {
            this.collectJunk(incomingBuffer);
            // Return an empty buffer
            return Buffer.buffer();
        } else {
            // We wait for result to be available
            // TODO: is that what we need?
            return this.processBufferResult(incomingBuffer).result();
        }
    }

    /**
     * Function to call before internalBuffer is written to the result - needed
     * to deal with junked results
     */
    protected Future<Buffer> filterEnd() {
        // Fill the internal buffer with the transformation
        return (this.chunkCollector != null)
                ? this.processBufferResult(this.chunkCollector)
                : Future.failedFuture("No buffer content");
    }

    protected abstract Future<Buffer> processBufferResult(Buffer incomingBuffer);

    private void collectJunk(final Buffer incomingBuffer) {
        if (this.chunkCollector == null) {
            this.chunkCollector = Buffer.buffer(incomingBuffer.length() * 2);
        }
        this.chunkCollector.appendBuffer(incomingBuffer);

    }

}
