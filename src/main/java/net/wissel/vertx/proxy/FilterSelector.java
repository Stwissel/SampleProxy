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
package net.wissel.vertx.proxy;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 * @author stw Class that contains all Filters that can be applied to the
 *         returned content from a call to the server
 *
 */
public class FilterSelector implements Function<HttpRequestResponse,ProxyFilter> {
    
    private class FilterConfig {
        final public String name;
        final public String className;
        final public Collection<String> subfilters = new ArrayList<>();
        
        public FilterConfig(String fName, String className) {
            this.name = fName;
            this.className = className;
        }

        public void addSubfilters(JsonArray subfilters) {
           subfilters.forEach( sf -> {
               this.subfilters.add(String.valueOf(sf));
           });
        }
        
    }

	private final Map<String, FilterConfig> filterList = new HashMap<>();

	public FilterSelector(JsonObject config) {
	
		JsonArray filterNames = config.getJsonArray("filters");
		filterNames.forEach(o -> {
			final JsonObject filter = (JsonObject) o;
			final JsonArray subfilters = filter.getJsonArray("subfilters");
			FilterConfig fc = new FilterConfig(filter.getString("name").toLowerCase(), filter.getString("class"));
			fc.addSubfilters(subfilters);
			this.filterList.put(fc.name, fc);
		});
	}

	/**
	 * This is the main function that applies ONE filter per content type
	 * The filters are loaded based on the content type. So specific logic 
	 * needs to go into that filter class
	 */
	@Override
	public ProxyFilter apply(HttpRequestResponse requestresponse) {
	    HttpClientResponse response = requestresponse.response;
	    ProxyFilter result = null;
		boolean isChunked = this.getIsChunked(response);
		String content = response.headers().get("Content-Type").toLowerCase();
		content = content.contains(";") ? content.substring(0, content.indexOf(";")) : content;
		if (this.filterList.containsKey(content)) {
		    result = this.loadFilter(content, isChunked);
		} else {
		    result = this.getEmptyProxyFilter();
		}

		return result;

	} 
	
	public ProxyFilter getEmptyProxyFilter() {
	  return new ProxyFilter() {

          @Override
          public ReadStream<Buffer> apply(ReadStream<Buffer> t) {
              return t;
          }

          @Override
          public void end(WriteStream<Buffer> result) {
              // no action required
          }

        /**
         * @see net.wissel.vertx.proxy.ProxyFilter#addSubfilters(java.util.Collection)
         */
        @Override
        public void addSubfilters(Collection<String> subfilters) {
            // No action required
        }          
      };   
	}

	/**
	 * Check if a response was transmitted chunked - makes processing a little harder
	 * @param response the remote server response
	 * @return is it chunked
	 */
	private boolean getIsChunked(HttpClientResponse response) {
		List<String> te = response.headers().getAll("transfer-encoding");
		if (te != null) {
			for (String val : te) {
				if (val.contains("chunked")) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Loads an instance of a proxy filter
	 * 
	 * @param fName class name of the filter
	 * @param isChunked parameter for constructor
	 * @return an instance of ProxyFilter
	 */
	private ProxyFilter loadFilter(final String contentName, final boolean isChunked) {
		ProxyFilter result = null;
		FilterConfig fc = this.filterList.get(contentName);
		try {
			@SuppressWarnings("rawtypes")
			Constructor constructor =  Class.forName(fc.className).getConstructor(Boolean.TYPE);
			result = (ProxyFilter) constructor.newInstance(isChunked);
			result.addSubfilters(fc.subfilters);
		} catch (Exception e) {
			e.printStackTrace();
			// We don't touch the result then
			result = this.getEmptyProxyFilter();
		}
		return result;
	}

}
