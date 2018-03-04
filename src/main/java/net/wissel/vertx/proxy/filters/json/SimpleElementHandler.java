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

import io.vertx.core.json.JsonObject;
import net.wissel.vertx.proxy.filters.JsonSubFilter;

/**
 * Manipulates elements in in a JsonObject Possible actions are:
 *  - remove: Element get deleted
 *  - clear: Element gets replaced by an empty element (JsonObject/JsonArray)
 *   - mask: replace content with **** to some extend
 *
 * @author swissel
 *
 */
public class SimpleElementHandler extends AbstractElementHandler implements JsonSubFilter {

    //
    public SimpleElementHandler(final JsonObject parameters) {
        super(parameters);
    }
    
    

    @Override
    public void apply(JsonObject source) {
        super.apply(source);
        this.options.path.forEach(path -> {                      
            final Collection<Object> morituri = JsonSelector.getElementsByPath(source, path);
            this.handleResultCollection(path, morituri);
        });
    }



    @Override
    protected String getLeafName(final String path, final ElementHandlerOptions opts) {
        return ("".equals(path) || "/".equals(path) || (path.lastIndexOf("/") < 0))
                ? ""
                : path.substring(path.lastIndexOf("/"));
    }
}