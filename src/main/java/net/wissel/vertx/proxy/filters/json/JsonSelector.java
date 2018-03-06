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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility class to select trees of elements in a JsonObject e.g.
 * /world/continent/country/city Returns the JsonObject **containing** the
 * searched value so it can be updated or deleted
 *
 * Special Cases: "" or "/" always return the whole JsonObject "*" is a
 * placeholder for all elements inside a JsonObject JsonArrays are automatically
 * traversed for
 *
 * @author swissel
 *
 */
public class JsonSelector {

	public static Logger logger = LoggerFactory.getLogger("JsonSelector");
	
	public static Collection<Object> getElementsByName(final JsonObject source, final String elementName) {
		return JsonSelector.getElementsByName(source, elementName, null);
	}

	/**
	 * Retrieves a JsonObject from anywhere in the hierarchy tree based on the name
	 * Object must be a JsonObject
	 *
	 * @param source
	 *            the original object
	 * @param elementName
	 *            name of the element to look for
	 * @param attributeName
	 *            optional name of an attribute the element needs
	 * @return Collection of JsonObject
	 */
	public static Collection<Object> getElementsByName(final JsonObject source, final String elementName,
			final String attributeName) {
		final Collection<Object> result = new ArrayList<>();
		JsonSelector.retrieveElements(source, elementName, attributeName, result);
		return result;
	}

	/**
	 * Retrieves a collection of Json Elements based on a full path from the
	 * document root Special case is element name * = any element. Full path needs
	 * to be given like
	 *
	 * e.g. /world/continent/* /city would match /world/continent/country/city and
	 * /world/continent/territory/city
	 *
	 * @param source
	 *            the JsonObject
	 * @param path
	 *            the path expression
	 * @return Collection of JsonObject
	 */
	public static Collection<Object> getElementsByPath(final JsonObject source, final String path) {
		final Collection<Object> result = new ArrayList<>();
		// Special case if path is empty or points to the root
		// We return the whole object
		if ((path == null) || "".equals(path) || "/".equals(path)) {
			result.add(source);
			return result;
		}

		// All other cases
		final String[] paraChain = path.split("/");
		final Queue<String> jsonPath = new LinkedList<>(Arrays.asList(paraChain));
		// We need to get rid of a leading element when the path was defined as
		// a leading slash: e.g. /somelement/someotherelement/moreelements
		if ("".equals(jsonPath.peek())) {
			jsonPath.poll();
		}

		// Recursive call to travel down the rabbit hole
		JsonSelector.retrievePathElements(source, jsonPath, result);
		return result;
	}

	private static void retrieveElements(JsonObject incomingJo, String elementName, String attributeName,
			Collection<Object> result) {
		incomingJo.forEach(entry -> {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof JsonObject) {
				JsonObject jo = (JsonObject) value;
				if (key.equals(elementName) && ((attributeName == null) || jo.containsKey(attributeName))) {
					result.add(jo);
				} else {
					JsonSelector.retrieveElements(jo, elementName, attributeName, result);
				}

			} else if (value instanceof JsonArray) {
				JsonArray ja = (JsonArray) value;
				if (key.equals(elementName)) {
					ja.forEach(jaEntry -> {
						if (jaEntry instanceof JsonArray) {
							JsonArray nextJa = (JsonArray) jaEntry;
							JsonSelector.retrieveElementsFromArray(nextJa, elementName, attributeName, result);
						} else if (jaEntry instanceof JsonObject) {
							JsonObject joNext = (JsonObject) jaEntry;
							if (key.equals(elementName)
									&& ((attributeName == null) || joNext.containsKey(attributeName))) {
								result.add(joNext);
							} else {
								JsonSelector.retrieveElements(joNext, elementName, attributeName, result);
							}
						}
					});

				} else {
					JsonSelector.retrieveElementsFromArray(ja, elementName, attributeName, result);
				}
			}
		});
	}

	private static void retrieveElementsFromArray(JsonArray ja, String elementName, String attributeName,
			Collection<Object> result) {
		ja.forEach(entry -> {
			if (entry instanceof JsonObject) {
				JsonObject jo = (JsonObject) entry;
				JsonSelector.retrieveElements(jo, elementName, attributeName, result);
			} else if (entry instanceof JsonArray) {
				JsonArray nextJa = (JsonArray) entry;
				JsonSelector.retrieveElementsFromArray(nextJa, elementName, attributeName, result);
			}
		});

	}

	/**
	 *
	 * @param source
	 *            - a JsonObject to be checked
	 * @param jsonPath
	 *            - the stack to be evaluated
	 * @param result
	 *            - the Collection of results
	 */
	private static void retrievePathElements(final JsonObject source, final Queue<String> jsonPath,
			final Collection<Object> result) {
		if (jsonPath.peek() == null) {
			return; // Nothing to do
		}

		// Now check what we got
		final String elementName = jsonPath.poll();

		// Special case * -> all JsonObjects contained in the source with double
		// traversal of
		// JsonArrays in JsonArrays
		if ("*".equals(elementName)) {
			source.forEach(entry -> {
				Object o = entry.getValue();
				if (o instanceof JsonObject) {
					JsonSelector.retrievePathElements((JsonObject) o, jsonPath, result);
				} else if (o instanceof JsonArray) {
					JsonSelector.retrievePathElementsFromArray((JsonArray) o, jsonPath, result);
				}
			});
			return;
		}

		// Check if we have a hit
		if (!source.containsKey(elementName)) {
			JsonSelector.logger.info("No element found:" + elementName);
			return;
		}

		// Depending on where we are in the jsonPath
		// the next action is determined
		// if this was the last item in the path we return the source
		// so the value can be altered. If we have more items in the
		// path we dig deeper
		if (jsonPath.peek() == null) {
			// Bottom level reached - add the result
			result.add(source);
		} else {
			final Object found = source.getValue(elementName);
			if (found instanceof JsonObject) {
				// Linear go deeper
				JsonSelector.retrievePathElements(source, jsonPath, result);
			} else if (found instanceof JsonArray) {
				// Go through the array individually - need
				retrievePathElementsFromArray((JsonArray) found, jsonPath, result);
			} else {
				JsonSelector.logger.info("Non JSON element encountered in middle of path:" + elementName);
			}
		}
	}

	/**
	 * Retrieves path elements from an Array - special challenge: Can't use up a
	 * single stack, need to clone it for each element in an array. Can't clone it
	 * inside the foreach array since execution isn't synchronous
	 *
	 * @param array
	 *            JsonArray with source elements
	 * @param jsonPath
	 *            the original Path stack
	 * @param result
	 *            the collection with resulting elements
	 */
	private static void retrievePathElementsFromArray(JsonArray array, Queue<String> jsonPath,
			Collection<Object> result) {
		// Each object gets its own Stach
		Map<JsonObject, Queue<String>> objectAndQueue = new HashMap<>();

		// Fill the map for processing with each JsonObject and a copy of the Stack
		array.stream().filter(a -> (a instanceof JsonObject)).map(o -> (JsonObject) o)
				.forEach(jo -> objectAndQueue.put(jo, new LinkedList<>(jsonPath)));

		// Now execute
		objectAndQueue.forEach((jo, stack) -> retrievePathElements(jo, stack, result));
	}
}
