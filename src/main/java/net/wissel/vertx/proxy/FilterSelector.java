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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
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
public class FilterSelector implements Function<HttpRequestResponse, ProxyFilter> {

    private class FilterConfig {
        final public String             mimeType;
        final public String             path;
        final public boolean            regex;
        final public String             className;
        final public Collection<JsonObject> subfilters = new ArrayList<>();

        public FilterConfig(JsonObject filter) {
            this.mimeType = filter.getString("mimeType").toLowerCase();
            this.path = filter.getString("path");
            this.regex = filter.getBoolean("regex", false);
            this.className = filter.getString("class");
            final JsonArray subfilters = filter.getJsonArray("subfilters");
            this.addSubfilters(subfilters);
        }

        public void addSubfilters(JsonArray subfilters) {
            if (subfilters != null) {
                subfilters.forEach(sf -> {
                    this.subfilters.add((JsonObject) sf);
                });
            }
        }
    }

    private final Map<String, Map<String, FilterConfig>> filterList = new HashMap<>();
    private final Vertx vertx;

    public FilterSelector(final Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        JsonArray filterNames = config.getJsonArray("filters");
        filterNames.forEach(o -> {
            final JsonObject filter = (JsonObject) o;
            FilterConfig fc = new FilterConfig(filter);
            this.addFilter(fc);
        });
    }

    /**
     * Adds the list of filters based on mime-type and URL URL can be * for any
     * or a regEx
     * 
     * @param fc
     */
    private void addFilter(FilterConfig fc) {
        String name = fc.mimeType;
        Map<String, FilterConfig> fMap = (this.filterList.containsKey(name))
                ? this.filterList.get(name)
                : new HashMap<>();
        fMap.put(fc.path, fc);
        this.filterList.put(name, fMap);
    }

    /**
     * This is the main function that applies ONE filter per content type The
     * filters are loaded based on the content type. So specific logic needs to
     * go into that filter class
     */
    @Override
    public ProxyFilter apply(HttpRequestResponse requestresponse) {
        HttpClientResponse response = requestresponse.response;
        HttpClientRequest request = requestresponse.request;
        String url = request.uri();
        boolean isChunked = this.getIsChunked(response);
        String content = response.headers().get("Content-Type").toLowerCase();
        content = content.contains(";") ? content.substring(0, content.indexOf(";")) : content;
        return this.loadFilter(content, url, isChunked);

    }

    public ProxyFilter getEmptyProxyFilter() {
        return new ProxyFilter() {

            @Override
            public Future<ReadStream<Buffer>> apply(ReadStream<Buffer> t) {
                return Future.succeededFuture(t);
            }

            @Override
            public Future<Void> end(WriteStream<Buffer> result) {
                return Future.succeededFuture();
            }

            /**
             * @see net.wissel.vertx.proxy.ProxyFilter#addSubfilters(java.util.Collection)
             */
            @Override
            public void addSubfilters(Collection<JsonObject> subfilters) {
                // No action required
            }
        };
    }

    /**
     * Check if a response was transmitted chunked - makes processing a little
     * harder
     * 
     * @param response
     *            the remote server response
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
     * @param fName
     *            class name of the filter
     * @param isChunked
     *            parameter for constructor
     * @return an instance of ProxyFilter
     */
    private ProxyFilter loadFilter(final String contentName, final String url, final boolean isChunked) {
        ProxyFilter result = null;
        Map<String, FilterConfig> filterMap = this.filterList.get(contentName);
        if (filterMap != null) {
            for(Map.Entry<String, FilterConfig> entry : filterMap.entrySet()) {
                String path = entry.getKey();
                FilterConfig fc = entry.getValue();
                if ("*".equals(path) || path.equalsIgnoreCase(url) || this.filterMatch(fc, url) ) {
                    try {
                        @SuppressWarnings("rawtypes")
                        Constructor constructor = Class.forName(fc.className).getConstructor(Vertx.class, Boolean.TYPE);
                        result = (ProxyFilter) constructor.newInstance(this.vertx, isChunked);
                        result.addSubfilters(fc.subfilters);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                        // We don't touch the result then
                        result = this.getEmptyProxyFilter();
                    }
                }
            }            
    }
        return (result == null) ? this.getEmptyProxyFilter() : result;
    }

    private boolean filterMatch(FilterConfig fc, String url) {
        boolean result = false;
        result = "*".equals(fc.path);
        result = url.equalsIgnoreCase(fc.path) || result;
        result = (result) ? result : (fc.regex ? this.regexMatch(fc.path, url) : result); 
        return result;
    }

    private boolean regexMatch(String regex, String source) {
        // TODO Implement Regex matcher
        return false;
    }
}
