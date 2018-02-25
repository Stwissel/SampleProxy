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
 * Gets rid of all images
 *
 * @author swissel
 *
 */
public class DropLinks implements HtmlSubFilter {
    private final JsonObject parameters;

    public DropLinks(final JsonObject parameters) {
        this.parameters = parameters;
    }

    /**
     * @see net.wissel.vertx.proxy.filters.HtmlSubFilter#apply(org.jsoup.nodes.Document)
     */
    @Override
    public void apply(final Document doc) {
        final boolean fullRemove = this.parameters.getBoolean("fullremove", false);
        final Elements links = doc.getElementsByTag("a");
        links.forEach(e -> {
            if (fullRemove) {
                e.remove();
            } else {
                e.attr("href", "#");
            }
        });
    }
}
