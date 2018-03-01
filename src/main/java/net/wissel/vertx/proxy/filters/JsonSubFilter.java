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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Manipulate a JsonObject or a JsonArray for data cleansing
 * @author swissel
 *
 */
public interface JsonSubFilter {
    public void apply(final JsonObject source);
    
    
    /**
     * Implementation for arrays - apply filter function for
     * all individual objects
     * 
     * @param arraySource The incoming array
     */
    public default void apply(final JsonArray arraySource) {
        arraySource.forEach(candidate -> {
            if (candidate instanceof JsonObject) {
                JsonObject json = (JsonObject) candidate;
                this.apply(json);
            }
        });
    }
}
