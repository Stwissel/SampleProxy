/********************************************************************************
 * Copyright (c) 2016, 2017 Julien Viet - https://github.com/vietj
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package net.wissel.vertx.proxy.impl;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CacheControl {

  private int maxAge;
  private boolean _public;

  public CacheControl parse(String header) {
    maxAge = -1;
    _public = false;
    String[] parts = header.split(","); // No regex
    for (String part : parts) {
      part = part.trim().toLowerCase();
      switch (part) {
        case "public":
          _public = true;
          break;
        default:
          if (part.startsWith("max-age=")) {
            maxAge = Integer.parseInt(part.substring(8));

          }
          break;
      }
    }
    return this;
  }

  public int maxAge() {
    return maxAge;
  }

  public boolean isPublic() {
    return _public;
  }

}
