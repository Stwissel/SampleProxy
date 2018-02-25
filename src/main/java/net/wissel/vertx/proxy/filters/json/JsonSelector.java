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
package net.wissel.vertx.proxy.filters.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility class to select trees of elements in a JsonObject e.g.
 * /filters/subfilters/class
 *
 * @author swissel
 *
 */
public class JsonSelector {

    public static Logger logger = LoggerFactory.getLogger("JsonSelector");

    public static Collection<Object> getRequestParam(final JsonObject source, final String path) {
        final Collection<Object> result = new ArrayList<>();
        final String[] paraChain = path.split("/");
        final Queue<String> jsonPath = new LinkedList<>(Arrays.asList(paraChain));
        // We need to get rid of a leading element when the path was defined as
        // a leading slash: e.g. /somelement/someotherelement/moreelements
        if ("".equals(jsonPath.peek())) {
            jsonPath.poll();
        }

        JsonSelector.retrieveElements(source, jsonPath, result);
        return result;
    }

    // TODO: introduce * as selector

    private static void retrieveElements(final JsonObject source, final Queue<String> jsonPath,
            final Collection<Object> result) {
        if (jsonPath.peek() == null) {
            return; // We are done
        }
        final String elementName = jsonPath.poll();
        if (!source.containsKey(elementName)) {
            JsonSelector.logger.info("No element found:" + elementName);
        }

        final Object found = source.getValue(elementName);

        // Check if we need parse deeper or we are done
        if (jsonPath.peek() == null) {
            // Bottom level reached - add the result
            result.add(found);
        } else {
            if (found instanceof JsonObject) {
                // Linear go deeper
                JsonSelector.retrieveElements(source, jsonPath, result);
            } else if (found instanceof JsonArray) {
                // Go through the array individually - need
                ((JsonArray) found).forEach(element -> {
                    if (element instanceof JsonObject) {
                        final Queue<String> newPath = new LinkedList<>(jsonPath);
                        final JsonObject foundJson = (JsonObject) element;
                        JsonSelector.retrieveElements(foundJson, newPath, result);
                    }
                });
            } else {
                JsonSelector.logger.info("Non JSON element encountered in middle of path:" + elementName);
            }

        }

    }
}
