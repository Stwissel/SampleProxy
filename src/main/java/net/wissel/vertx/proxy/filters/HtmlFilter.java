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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class HtmlFilter extends AbstractFilter {

    private final Collection<HtmlSubFilter> subfilters = new ArrayList<>();

    public HtmlFilter(final Vertx vertx, final boolean isChunked) {
        super(vertx, isChunked);
    }

    @Override
    public void addSubfilters(final Collection<JsonObject> subfilters) {
        if (subfilters == null || subfilters.isEmpty()) {
            return;
        }

        // Loads all classes for subfilters of html send parameters into class
        subfilters.forEach(f -> {
            try {
                @SuppressWarnings("rawtypes")
                final Constructor constructor = Class.forName(f.getString("class")).getConstructor(JsonObject.class);
                final HtmlSubFilter result = (HtmlSubFilter) constructor.newInstance(f.getJsonObject("parameters"));
                this.subfilters.add(result);
            } catch (final Exception e) {
                this.logger.error("Class not found: " + f, e);
            }
        });

    }

    @Override
    protected Future<Buffer> processBufferResult(final Buffer incoming) {

        final Future<Buffer> futureResult = Future.future();

        // Run in our own thread the actual filters working
        // on a Jsoup document to execute whatever we have in mind
       this.getVertx().executeBlocking(future -> {
            final Document doc = Jsoup.parse(incoming.toString());
            this.subfilters.forEach(sf -> {
                sf.apply(doc);
            });
            final Buffer b = Buffer.buffer(doc.outerHtml());
            future.complete(b);
        }, result -> {
            if (result.succeeded()) {
                final Buffer b = (Buffer) result.result();
                futureResult.complete(b);
            } else {
                futureResult.fail(result.cause());
            }
        });

        return futureResult;

    }

}
