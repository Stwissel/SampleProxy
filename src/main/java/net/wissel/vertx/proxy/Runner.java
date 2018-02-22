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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.function.Consumer;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Runner {

	static Logger logger = LoggerFactory.getLogger(Runner.class);

	public static void runVerticle(final String verticleID, final boolean debugMode) {
        
	    final JsonObject customValues = Runner.loadVerticleConfigOptions();
        // Indicator it was started by the runner
        
	    customValues.put("RunnerLoaded", true);
		// That's just for IDE testing...
		final Consumer<Vertx> runner = vertx -> {
			try {
				final DeploymentOptions depOpt = new DeploymentOptions();
				depOpt.setConfig(customValues);
				vertx.deployVerticle(verticleID, depOpt, res -> {
					if (res.succeeded()) {
						Runner.logger.info(verticleID + " deployed as " + res.result());
					} else {
						Runner.logger.error("Deployment failed for " + verticleID);
					}
				});
			} catch (final Throwable t) {
				Runner.logger.error(t);
			}
		};

		final VertxOptions options = new VertxOptions();
		if (Runner.isDebug(debugMode, customValues)) {
			options.setBlockedThreadCheckInterval(1000 * 60 * 60);
		}

		final Vertx vertx = Vertx.vertx(options);
		runner.accept(vertx);
	}
	
	public static JsonObject loadVerticleConfigOptions() {
        JsonObject customValues = new JsonObject();
        String configFileName = "config.json";
        InputStream in = null;
        try {
            File configFile = new File(configFileName);
            System.out.println("Looking for config " + configFile.getAbsolutePath());
            if (configFile.exists()) {
                in = new FileInputStream(configFile);
                final Scanner scanner = new Scanner(in);
                scanner.useDelimiter("\\Z");
                String rawJson = scanner.next();
                scanner.close();
                customValues = new JsonObject(rawJson);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return customValues;
    }

	private static boolean isDebug(boolean debugMode, JsonObject customValues) {
		// Debug was given as parameter or there's an environment variable
		// set to true so we go debugging
		return debugMode || Boolean.parseBoolean(System.getenv("Debug")) || customValues.getBoolean("DEBUG", false);
	}
}
