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

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author swissel
 *
 */
public class JsonXMLTools {

    public static Document getDocument(final JsonObject source) {
        final SimpleXMLDoc sd = new SimpleXMLDoc();
        try {
            sd.openTag("data");
            JsonXMLTools.parseToOutput(source, sd);
            return sd.closeDocument();
        } catch (TransformerConfigurationException | SAXException e) {
            LoggerFactory.getLogger(JsonXMLTools.class).error(e.getMessage(), e);
        }
        return null;
    }

    public static JsonObject getJson(final Document doc) {

        try {
            final Xml2JsonHandler dh = new Xml2JsonHandler();
            final SAXResult saxResult = new SAXResult(dh);
            final DOMSource domSource = new DOMSource(doc);
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(domSource, saxResult);
            return dh.getJson();
        } catch (TransformerFactoryConfigurationError | TransformerException e) {
            LoggerFactory.getLogger(JsonXMLTools.class).error(e.getMessage(), e);
        }

        return null;
    }

    private static void extractValue(final String key, final Object value, final SimpleXMLDoc sd) throws Exception {
        if (value instanceof JsonObject) {
            final JsonObject nextJson = (JsonObject) value;
            sd.openTag("element", "name", key);
            JsonXMLTools.parseToOutput(nextJson, sd);
            sd.closeTag(1);
        } else if (value instanceof JsonArray) {
            final JsonArray oneArray = (JsonArray) value;
            sd.openTag("array", "name", key);
            oneArray.forEach(arrayElement -> {
                try {
                    JsonXMLTools.extractValue(key, arrayElement, sd);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
            sd.closeTag(1);
        } else {
            final Map<String, String> attributes = JsonXMLTools.getAttributeMap(key, value);
            sd.addEmptyTag("element", attributes);
        }
    }

    private static Map<String, String> getAttributeMap(final String key, final Object value) {
        final Map<String, String> result = new HashMap<>();
        result.put("name", key);
        result.put("value", String.valueOf(value));
        result.put("type", value.getClass().getSimpleName());
        return result;
    }

    private static void parseToOutput(final JsonObject json, final SimpleXMLDoc sd) {
        json.forEach(entry -> {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            try {
                JsonXMLTools.extractValue(key, value, sd);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

}
