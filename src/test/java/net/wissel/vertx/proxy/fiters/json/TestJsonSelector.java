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
package net.wissel.vertx.proxy.fiters.json;

import java.io.PrintStream;
import java.util.Collection;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.wissel.vertx.proxy.filters.json.JsonSelector;

/**
 * @author swissel
 *
 */
public class TestJsonSelector {

    /**
     * @param args
     */
    public static void main(String[] args) {
        TestJsonSelector jts = new TestJsonSelector();
        Vertx vertx = Vertx.vertx();
        Buffer b = vertx.fileSystem().readFileBlocking("src/test/resources/sample.json");
        JsonObject json = new JsonObject(b);
        jts.runTestSuite(json);
        System.out.println("Done");
    }

    private void runTestSuite(final JsonObject json) {
        this.runOneTest(json, "/");
        this.runOneTest(json, "/enemies");
        this.runOneTest(json, "/movie");
        this.runOneTest(json, "/bla");
        this.runOneTest(json, "/enemies/name");
    }
    
    private void runOneTest(final JsonObject json, final String path) {
        this.echoFindings(json, path, System.out);
    }
    
    private void echoFindings(final JsonObject json, final String path, final PrintStream out) {
        out.println("--------------"+path+"--------------");
        Collection<Object> results = JsonSelector.getRequestParam(json, path);
        out.println("Number of elements found:"+results.size());
        results.forEach(o -> {
            out.println(o.getClass().getName());
            if (o instanceof JsonObject) {
                out.println(((JsonObject) o).encodePrettily());
            } else if (o instanceof JsonArray) {
                out.println(((JsonArray) o).encodePrettily());
            } else {
                out.println(o);
            }
        });
    }

}
