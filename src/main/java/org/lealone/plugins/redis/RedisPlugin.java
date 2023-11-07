/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package org.lealone.plugins.redis;

import java.util.Map;

import org.lealone.db.PluginBase;
import org.lealone.plugins.redis.server.RedisServerEngine;

public class RedisPlugin extends PluginBase {

    public RedisPlugin() {
        super(RedisServerEngine.NAME);
    }

    @Override
    public void init(Map<String, String> config) {
        super.init(config);
    }

    @Override
    public void close() {
    }
}
