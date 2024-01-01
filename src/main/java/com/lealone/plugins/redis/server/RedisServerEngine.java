/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package com.lealone.plugins.redis.server;

import com.lealone.server.ProtocolServer;
import com.lealone.server.ProtocolServerEngineBase;

public class RedisServerEngine extends ProtocolServerEngineBase {

    public static final String NAME = "Redis";

    public RedisServerEngine() {
        super(NAME);
    }

    @Override
    protected ProtocolServer createProtocolServer() {
        return new RedisServer();
    }
}
