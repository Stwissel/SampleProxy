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

import java.util.Collection;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.wissel.vertx.proxy.filters.JsonSubFilter;

/**
 * get rid of Json elements based on a JSON path
 * 
 * @author swissel
 *
 */
public class MaskElements implements JsonSubFilter {
    private final JsonObject parameters;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public MaskElements(final JsonObject parameters) {
        this.parameters = parameters;
    }
    @Override
    public void apply(final JsonObject source) {
        String path = this.parameters.getString("path");

        if (path == null) {
            // Nothing to mask
            return;
        }
        
        // To change a value we need to retrieve the element above
        // to be able to pull out getValue and put
        String pathAbove = path.substring(0,path.lastIndexOf("/"));
        String valueName = path.substring(path.lastIndexOf("/"));

        Collection<Object> morituri = JsonSelector.getElementsByPath(source, pathAbove);
      
        morituri.forEach(mori -> {
            if (mori instanceof JsonObject) {
                JsonObject json = (JsonObject) mori;
                maskJsonValue(valueName, json);
            } else if (mori instanceof JsonArray) {
                ((JsonArray) mori).forEach(ele -> {
                    if (ele instanceof JsonObject) {
                        JsonObject json = (JsonObject) ele;
                        maskJsonValue(valueName, json);
                    }
                });
            } else {
                this.logger.info("Encountered on JSONPath:"+mori.getClass().getName());
            }
        });


    }
    private void maskJsonValue(String valueName, JsonObject json) {
        String curVal = String.valueOf(json.getValue(valueName));
        json.put(valueName, this.maskValue(curVal));
    }
    
    
    private String maskValue(final String curVal) {
        // TODO fix this properly
        return "***"+curVal;
    }

}
