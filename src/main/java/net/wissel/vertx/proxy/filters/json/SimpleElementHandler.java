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
import java.util.Collection;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.wissel.vertx.proxy.filters.JsonSubFilter;

/**
 * Manipulates elements in in a JsonObject Possible actions are: - remove:
 * Element get deleted - clear: Element gets replaced by an empty element
 * (JsonObject/JsonArray) - mask: replace content with **** to some extend
 * 
 * @author swissel
 *
 */
public class SimpleElementHandler implements JsonSubFilter {

    // Processing options
    private class SimpleElementHandlerOptions {
        public final String             operation;
        public final int                unmaskedCount;
        public final String             maskPattern;
        public final Collection<String> path = new ArrayList<>();

        public SimpleElementHandlerOptions(final JsonObject parameters) {
            this.operation = parameters.getString("action", "remove");
            this.unmaskedCount = parameters.getInteger("unmaskedCount", 2);
            this.maskPattern = parameters.getString("maskPattern", "****");
            try {
                JsonArray pathArray = parameters.getJsonArray("path");
                pathArray.forEach(a -> path.add(String.valueOf(a)));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private final SimpleElementHandlerOptions options;
    private final Logger                      logger = LoggerFactory.getLogger(this.getClass());

    public SimpleElementHandler(final JsonObject parameters) {
        this.options = new SimpleElementHandlerOptions(parameters);
    }

    @Override
    public void apply(final JsonObject source) {

        if (this.options.path.isEmpty()) {
            source.clear();
            return;
        }

        this.options.path.forEach(path -> {
            // the leafName is the actual element we are after
            final String leafName = ("".equals(path) || "/".equals(path) || path.lastIndexOf("/") < 0)
                    ? ""
                    : path.substring(path.lastIndexOf("/"));
            Collection<Object> morituri = JsonSelector.getRequestParam(source, path);
            morituri.stream()
                    .filter(mori -> (mori instanceof JsonObject))
                    .forEach(mori -> {
                        JsonObject json = (JsonObject) mori;
                        switch (this.options.operation) {
                            case "clear":
                                this.clearContent(json, leafName);
                                break;
                            case "mask":
                                this.maskContent(json, leafName);
                                break;

                            default:
                                this.removeContent(json, leafName);
                                break;
                        }
                    });
        });
    }

    private void removeContent(JsonObject json, String leafName) {
        json.remove(leafName);
    }

    // Masks the content - turn it into String
    private void maskContent(JsonObject json, String leafName) {
        Object toBemasked = json.getValue(leafName);
        
        // Capture null value
        if (toBemasked == null) {
            // Shouldn't happen, but whatever...
            json.put(leafName, this.options.maskPattern);
            return;
        }
        
        // Catch arrays
        if (toBemasked instanceof JsonArray) {
            JsonArray newArray = new JsonArray();
            JsonArray array = (JsonArray) toBemasked;
            array.forEach(a -> newArray.add(this.maskString(a, this.options.maskPattern, this.options.unmaskedCount)));
            json.put(leafName,newArray);
        }
        
        // Scalar values
        json.put(leafName, this.maskString(toBemasked, this.options.maskPattern, this.options.unmaskedCount)); 
        
    }
    
    private String maskString(final Object objectSource, final String pattern, final int length) {
        String source = String.valueOf(objectSource);
        StringBuilder result = new StringBuilder();
        
        if (length < 1 || source.length() <= length) {
            result.append(pattern);
        } else {
            if (source.indexOf("@") > -1) {
                // Email needs special treatment
                result.append(source.substring(0, length));
                result.append(pattern);
                result.append("@");
                result.append(pattern);
                result.append(source.substring(source.length()-length));
            } else {
                // Regular String
            result.append(source.substring(0, length));
            result.append(pattern);
            result.append(source.substring(source.length()-length));
            }
        }
        
        return result.toString();
    }

    // Leaves the element in place but removes the value
    // String, JsonObjects, JsonArray
    private void clearContent(JsonObject json, String leafName) {
        Object removed = json.remove(leafName);
        if (removed != null) {
            if (removed instanceof JsonObject) {
                   json.put(leafName, new JsonObject());
            } else  if (removed instanceof JsonArray) {
                json.put(leafName, new JsonArray());
            }  if (removed instanceof String) {
                json.put(leafName, "");
            }
            // TODO: to we need to "clear" numeric values?
        }
        
    }
}