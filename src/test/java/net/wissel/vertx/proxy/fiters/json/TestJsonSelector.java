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
		System.out.println("Done Part 1");

		Buffer b2 = vertx.fileSystem().readFileBlocking("src/test/resources/sample2.json");
		JsonObject json2 = jts.getJsonObject(b2);
		jts.runTestSuite2(json2);
		System.out.println("Done part 2");
	}

	private void echoFindings(final JsonObject json, final String path, final PrintStream out) {
		out.println("--------------" + path + "--------------");
		Collection<Object> results = JsonSelector.getElementsByPath(json, path);
		out.println("Number of elements found:" + results.size());
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

	private void echoFindings2(final JsonObject json, final String elementName, final String attributeName, final PrintStream out) {
		out.println("--------------" + elementName + "--------------");
		Collection<Object> results = JsonSelector.getElementsByName(json, elementName, attributeName);
		out.println("Number of elements found:" + results.size());
		results.forEach(o -> {
			if (o instanceof JsonObject) {
				out.println(((JsonObject) o).encodePrettily());
			} else {
				out.println(o);
			}
		});
	}

	private void runOneTest(final JsonObject json, final String path) {
		echoFindings(json, path, System.out);
	}

	private void runOneTest2(final JsonObject json, final String elementName, final String attributeName) {
		echoFindings2(json, elementName, attributeName, System.out);
	}

	private void runTestSuite(final JsonObject json) {
		runOneTest(json, "/");
		runOneTest(json, "/enemies");
		runOneTest(json, "/movie");
		runOneTest(json, "/bla");
		runOneTest(json, "/enemies/name");
	}

	private void runTestSuite2(final JsonObject json) {
		runOneTest2(json, "Email", "value");
		runOneTest2(json, "Phone", "value");
	}
	
	private JsonObject getJsonObject(final Buffer incoming) {
        int i = 0;

        while (!incoming.getString(i,i+1).equals("{")) {
            i++;
        }

        return new JsonObject((i != 0) ? incoming.getBuffer(i, incoming.length()) : incoming);
    }

}
