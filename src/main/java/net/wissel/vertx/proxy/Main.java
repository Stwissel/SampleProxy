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

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;

/**
 * @author swissel
 *
 */
public class Main extends AbstractVerticle {

	/**
	 * @param args
	 *            Gets ignored here
	 */
	public static void main(String[] args) {
		Runner.runVerticle(Main.class.getName(), true);
	}

	private JsonObject params;
	private Integer port;
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	/**
	 * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
	 */
	@Override
	public void start(Future<Void> startFuture) throws Exception {
		// Configuration
		ConfigStoreOptions fileStore = new ConfigStoreOptions().setType("file")
				.setConfig(new JsonObject().put("path", "proxy.json"));
		ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore);
		ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

		retriever.getConfig(result -> {
			if (result.failed()) {
				result.cause().printStackTrace();
				startFuture.fail(result.cause());
			} else {
				this.params = result.result();
				String portCandidate = System.getenv("PORT");
				this.params.put("port", (portCandidate != null) ? Integer.valueOf(portCandidate)
						: this.params.getInteger("port", 8080));
				this.port = this.params.getInteger("port");
				this.launchProxy(startFuture);
			}
		});
	}

	private void launchProxy(Future<Void> startFuture) {
	    
	    HttpClientOptions clientOptions = new HttpClientOptions().setMaxInitialLineLength(10000).setLogActivity(true)
                .setTryUseCompression(true);
	    
	    if (this.config().getBoolean("useProxy", false)) {
	        ProxyOptions proxyOptions = new ProxyOptions()
	                .setHost(this.params.getString("proxyHost", "localhost"))
	                .setPort(this.params.getInteger("proxyPort", 8080))
	                .setType(ProxyType.HTTP);
	        clientOptions.setProxyOptions(proxyOptions);
        }
	    
	    if (this.params.getBoolean("useSSL", true)) {
	        clientOptions.setSsl(true).setTrustAll(true);
	    }
	    final HttpClient client = vertx.createHttpClient(clientOptions);
		final SfdcHttpProxy proxy = SfdcHttpProxy
		        .reverseProxy(client)
		        .target(this.params.getInteger("targetPort", 80), this.params.getString("targetHost","www.spiegel.de"));
		proxy.filterSelector(new FilterSelector(this.params));
		
		HttpServerOptions serverOptions = new HttpServerOptions()
		        .setPort(this.port)
		        .setMaxInitialLineLength(10000)
				.setLogActivity(true)
				.setCompressionSupported(true);

		final HttpServer proxyServer = vertx.createHttpServer(serverOptions).requestHandler(req -> {
			this.logger.info(req.path());
			proxy.handle(req);
		});
		
		
		proxyServer.listen(ar -> {
			if (ar.succeeded()) {
				this.logger.info("Proxy server started on " + this.port);
				startFuture.complete();
			} else {
				ar.cause().printStackTrace();
				startFuture.fail(ar.cause());
			}
		});

	}

}
