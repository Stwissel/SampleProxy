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
package net.wissel.vertx.proxy.filters.html;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import io.vertx.core.json.JsonObject;
import net.wissel.vertx.proxy.filters.HtmlSubFilter;

/**
 * Removes all link targets URLs from a page
 * @author swissel
 *
 */
public class DropElements implements HtmlSubFilter {
    
    private final JsonObject parameters;
    
    public DropElements(final JsonObject parameters) {
        this.parameters = parameters;
    }

    @Override
    public void apply(Document doc) {
        
        // When no tagname is supplied we drop the body tag
        // crude but efficient to test
        final String tagName = this.parameters.getString("name", "body");
        
        Elements elements = doc.getElementsByTag(tagName);
        elements.forEach(e -> {
            e.remove();
        });

    }

}
