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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.vertx.core.json.JsonObject;
import net.wissel.vertx.proxy.filters.JsonSubFilter;

/**
 * The PATH is an XPATH expression followed by a | character and the name of the
 * element to look for eg /element[@name='enemies' and element/@name='lair'] ->
 * select all enemies that have a lair from { enemies: [ 'Venom', {'lair' :
 * 'dungeon', 'name' : 'Sandman'}] } you get {'lair' : 'dungeon', 'name' :
 * 'Sandman'}
 *
 * @author swissel
 *
 */
public class XPathElementHandler extends AbstractElementHandler implements JsonSubFilter {

    public XPathElementHandler(final JsonObject parameters) {
        super(parameters);
    }

    @Override
    public void apply(final JsonObject source) {
        super.apply(source);
        final Document doc = JsonXMLTools.getDocument(source);
        // Process everything in DOM land
        this.options.path.forEach(path -> {
            final String realPath = path.substring(0, path.lastIndexOf("|"));
            this.handleDomResults(doc, realPath, this.options);
        });
        // Back from DOM land to JSON land
        JsonXMLTools.getJson(doc);
    }

    @Override
    protected String getLeafName(final String path, final ElementHandlerOptions opts) {
        final int split = path.lastIndexOf("|");
        if (split < 0) {
            return "undefined";
        } else {
            return path.substring(split);
        }
    }

    private void handleDomResults(final Document doc, final String realPath, final ElementHandlerOptions options) {
        final XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            final NodeList nodes = (NodeList) xPath.evaluate(realPath, doc.getDocumentElement(),
                    XPathConstants.NODESET);
            // Walk backwards since we might delete some!
            for (int i = nodes.getLength(); i > 0; i--) {
                final Node curNode = nodes.item(i - 1);
                this.handleResultNode(curNode, options);
            }
        } catch (final XPathExpressionException e) {
            this.logger.error(e.getMessage(), e);
        }

    }

    private void handleResultNode(final Node curNode, final ElementHandlerOptions options) {

        switch (options.operation) {
            case "mask":
                curNode.setNodeValue(
                        this.maskString(curNode.getNodeValue(), options.maskPattern, options.unmaskedCount));
                break;
            case "clear":
                if (curNode.getNodeType() == Node.ATTRIBUTE_NODE) {
                    curNode.setNodeValue("");
                } else {
                    while (curNode.getFirstChild() != null) {
                        curNode.removeChild(curNode.getFirstChild());
                    }
                }
                break;
            default:
                // Remove
                curNode.getParentNode().removeChild(curNode);
                break;
        }
    }

}
