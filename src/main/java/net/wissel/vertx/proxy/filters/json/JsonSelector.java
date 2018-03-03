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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility class to select trees of elements in a JsonObject e.g.
 * /world/continent/country/city
 * Returns the JsonObject **containing** the searched value
 * so it can be updated or deleted
 * 
 * Special Cases:
 *  "" or "/" always return the whole JsonObject
 *  "*" is a placeholder for all elements inside a JsonObject
 *  JsonArrays are automatically traversed for 
 *
 * @author swissel
 *
 */
public class JsonSelector {

    public static Logger logger = LoggerFactory.getLogger("JsonSelector");


    public static Collection<Object> getElementsByPath(final JsonObject source, final String path) {
        final Collection<Object> result = new ArrayList<>();
        // Special case if path is empty or points to the root
        // We return the whole object
        if (path == null || "".equals(path) || "/".equals(path)) {
            result.add(source);
            return result;
        }
        
        // All other cases
        final String[] paraChain = path.split("/");
        final Queue<String> jsonPath = new LinkedList<>(Arrays.asList(paraChain));
        // We need to get rid of a leading element when the path was defined as
        // a leading slash: e.g. /somelement/someotherelement/moreelements
        if ("".equals(jsonPath.peek())) {
            jsonPath.poll();
        }
        
        // Recursive call to travel down the rabbit hole
        JsonSelector.retrieveElements(source, jsonPath, result);
        return result;
    }

    /**
     * 
     * @param source - a JsonObject to be checked
     * @param jsonPath - the stack to be evaluated
     * @param result - the Collection of results
     */
    private static void retrieveElements(final JsonObject source, final Queue<String> jsonPath,
            final Collection<Object> result) {
        if (jsonPath.peek() == null) {
            return; // Nothing to do
        }
        
        // Now check what we got
        final String elementName = jsonPath.poll();
        
        // Special case * -> all JsonObjects contained in the source with double traversal of
        // JsonArrays in JsonArrays
        if ("*".equals(elementName)) {
            source.forEach(entry -> {
                Object o = entry.getValue();
                if (o instanceof JsonObject) {
                    JsonSelector.retrieveElements((JsonObject) o, jsonPath, result);
                } else if (o instanceof JsonArray) {
                    JsonSelector.retrieveElementsFromArray((JsonArray) o, jsonPath, result);
                }
            });
            return;
        }
        
        // Check if we have a hit
        if (!source.containsKey(elementName)) {
            JsonSelector.logger.info("No element found:" + elementName);
            return;
        }
        
        // Depending on where we are in the jsonPath
        // the next action is determined
        // if this was the last item in the path we return the source
        // so the value can be altered. If we have more items in the
        // path we dig deeper
        if (jsonPath.peek() == null) {
            // Bottom level reached - add the result
            result.add(source);
        } else {
            final Object found = source.getValue(elementName);
            if (found instanceof JsonObject) {
                // Linear go deeper
                JsonSelector.retrieveElements(source, jsonPath, result);
            } else if (found instanceof JsonArray) {
                // Go through the array individually - need
               retrieveElementsFromArray((JsonArray) found, jsonPath, result);
            } else {
                JsonSelector.logger.info("Non JSON element encountered in middle of path:" + elementName);
            }
        }
    }

    /**
     * Retrieves path elements from an Array - special challenge: Can't use up a single
     * stack, need to clone it for each element in an array. Can't clone it inside the foreach
     * array since execution isn't synchronous
     * 
     * @param array JsonArray with source elements
     * @param jsonPath the original Path stack
     * @param result the collection with resulting elements
     */     
    private static void retrieveElementsFromArray(JsonArray array, Queue<String> jsonPath, Collection<Object> result) {
        // Each object gets its own Stach
        Map<JsonObject, Queue<String>> objectAndQueue = new HashMap<>();
        
        // Fill the map for processing with each JsonObject and a copy of the Stack
        array.stream()
        .filter(a -> (a instanceof JsonObject))
        .map(o -> (JsonObject) o)
        .forEach(jo -> objectAndQueue.put(jo, new LinkedList<>(jsonPath)));
        
        //Now execute
        objectAndQueue.forEach((jo, stack) -> retrieveElements(jo, stack, result));
    }
}
