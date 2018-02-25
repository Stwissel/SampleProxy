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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Filters out JSON after assembling it into a JsonObject
 * 
 * @author swissel
 *
 */
public class JsonFilter extends AbstractFilter {

    private Buffer                          chunkCollector = null;
    private final Collection<JsonSubFilter> subfilters     = new ArrayList<>();
    private final Logger                    logger         = LoggerFactory.getLogger(this.getClass());

    public JsonFilter(final boolean isChunked) {
        super(isChunked);
    }

    @Override
    public void addSubfilters(final Collection<JsonObject> subfilters) {
        if (subfilters == null) {
            return;
        }

        // Loads all classes for subfilters of html send parameters into class
        subfilters.forEach(f -> {
            try {
                @SuppressWarnings("rawtypes")
                Constructor constructor = Class.forName(f.getString("class")).getConstructor(JsonObject.class);
                final JsonSubFilter result = (JsonSubFilter) constructor.newInstance(f.getJsonObject("parameters"));
                this.subfilters.add(result);
            } catch (Exception e) {
                this.logger.error("Class not found: " + f, e);
            } 
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.wissel.salesforce.proxy.filters.AbstractFilter#filterBuffer(io.vertx.
     * core.buffer.Buffer)
     */
    @Override
    protected Buffer filterBuffer(final Buffer incomingBuffer) {
        if (this.isChunked) {
            this.collectJunk(incomingBuffer);
            // Return an empty buffer
            return Buffer.buffer();
        } else {
            return this.processJsonBuffer(incomingBuffer);
        }

    }

    @Override
    protected void filterEnd() {
        // Fill the internal buffer with the transformation
        if (this.chunkCollector != null) {
            this.appendInternalBuffer(this.processJsonBuffer(this.chunkCollector));
        }
    }

    private void collectJunk(final Buffer incomingBuffer) {
        if (this.chunkCollector == null) {
            this.chunkCollector = Buffer.buffer(incomingBuffer.length() * 2);
        }
        this.chunkCollector.appendBuffer(incomingBuffer);

    }

    private Buffer processJsonBuffer(final Buffer incoming) {
        // Presuming we need the same space
        final Buffer result = Buffer.buffer(incoming.length());
        final JsonObject json = new JsonObject(incoming);

        // Here goes the changes executed by subfilters
        this.subfilters.forEach(sf -> {
            sf.apply(json);
        });

        // And back out
        result.appendString(json.encode());
        return result;

    }

}
