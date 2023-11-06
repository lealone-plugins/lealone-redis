/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package org.lealone.plugins.redis.command;

import org.lealone.db.session.ServerSession;
import org.lealone.plugins.redis.server.RedisServerEngine;
import org.lealone.sql.SQLEngineBase;

public class RedisCommandEngine extends SQLEngineBase {

    public RedisCommandEngine() {
        super(RedisServerEngine.NAME);
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return RedisCommandParser.quoteIdentifier(identifier);
    }

    @Override
    public RedisCommandParser createParser(ServerSession session) {
        return new RedisCommandParser(session);
    }
}
