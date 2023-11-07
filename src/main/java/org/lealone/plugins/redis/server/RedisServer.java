/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package org.lealone.plugins.redis.server;

import java.util.Map;

import org.lealone.db.LealoneDatabase;
import org.lealone.net.WritableChannel;
import org.lealone.server.AsyncServer;
import org.lealone.server.Scheduler;

public class RedisServer extends AsyncServer<RedisServerConnection> {

    public static final String VERSION = "7.2.3";
    public static final int DEFAULT_PORT = 6379;

    @Override
    public String getType() {
        return RedisServerEngine.NAME;
    }

    @Override
    public void init(Map<String, String> config) {
        super.init(config);
    }

    @Override
    public synchronized void start() {
        super.start();
        // 创建默认的 redis 数据库
        String sql = "CREATE DATABASE IF NOT EXISTS redis PARAMETERS(PERSISTENT=true)";
        LealoneDatabase.getInstance().getSystemSession().prepareStatementLocal(sql).executeUpdate();
    }

    @Override
    protected int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected RedisServerConnection createConnection(WritableChannel writableChannel,
            Scheduler scheduler) {
        return new RedisServerConnection(this, writableChannel, scheduler);
    }

    @Override
    protected void beforeRegister(RedisServerConnection conn, Scheduler scheduler) {
    }
}
