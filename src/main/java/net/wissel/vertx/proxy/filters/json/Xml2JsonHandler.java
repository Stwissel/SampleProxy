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

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author swissel
 *
 */
public class Xml2JsonHandler extends DefaultHandler {
    private final Stack<Object> jStack = new Stack<>();
    private final JsonObject    json   = new JsonObject();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Xml2JsonHandler() {
        this.jStack.push(this.json);
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        // Get rid of one element
        this.jStack.pop();
    }

    public final JsonObject getJson() {
        return this.json;
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName, final String qName,
            final Attributes attributes)
            throws SAXException {
        // We don't process the data cover element
        if ("data".equals(qName)) {
            return;
        }

        // Get name and value from the element. Value might be null
        final String curName = attributes.getValue("name");
        final String curValueCandidate = attributes.getValue("value");
        final String curType = attributes.getValue("type");

        final Object curValue = this.extractValueWithType(curValueCandidate, curType);

        // The current object on the stack
        final Object o = this.jStack.peek();

        // Play through cases
        // Parent is object and element has a value
        if ((o instanceof JsonObject) && (curValue != null)) {
            ((JsonObject) o).put(curName, curValue);
            this.jStack.push(o);
        } else if ((o instanceof JsonObject) && (curValue == null) && "element".equals(qName)) {
            final JsonObject jo = new JsonObject();
            ((JsonObject) o).put(curName, jo);
            this.jStack.push(jo);
        } else if ((o instanceof JsonObject) && (curValue == null) && "array".equals(qName)) {
            final JsonArray ja = new JsonArray();
            ((JsonObject) o).put(curName, ja);
            this.jStack.push(ja);
        } else if ((o instanceof JsonArray) && (curValue != null)) {
            ((JsonArray) o).add(curValue);
            this.jStack.push(o);
        } else if ((o instanceof JsonArray) && (curValue == null) && "element".equals(qName)) {
            final JsonObject jo = new JsonObject();
            ((JsonArray) o).add(jo);
            this.jStack.push(jo);
        } else if ((o instanceof JsonArray) && (curValue == null) && "array".equals(qName)) {
            final JsonArray ja = new JsonArray();
            ((JsonArray) o).add(ja);
            this.jStack.push(ja);
        }
    }

    private Object extractValueWithType(String curValueCandidate, String curType) {
        Object result = null;
        try {
            if (curType != null) {
                switch (curType) {
                    case "Boolean":
                        result = Boolean.valueOf(curValueCandidate);
                        break;
                    case "Integer":
                        result = Integer.valueOf(curValueCandidate);
                        break;
                    case "Double":
                        result = Double.valueOf(curValueCandidate);
                        break;
                    case "Long":
                        result = Long.valueOf(curValueCandidate);
                        break;
                    default:
                        result = curValueCandidate;
                        break;
                }
            }
        } catch (Throwable t) {
           this.logger.error(t.getMessage(), t);

        }
        return (result == null) ? curValueCandidate : result;
    }
}
