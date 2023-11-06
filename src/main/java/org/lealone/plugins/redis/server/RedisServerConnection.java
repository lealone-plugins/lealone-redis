/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package org.lealone.plugins.redis.server;

import org.lealone.db.session.ServerSession;
import org.lealone.net.NetBuffer;
import org.lealone.net.WritableChannel;
import org.lealone.server.AsyncServerConnection;
import org.lealone.server.Scheduler;
import org.lealone.server.SessionInfo;

public class RedisServerConnection extends AsyncServerConnection {

    private final RedisServer server;
    private final Scheduler scheduler;
    private ServerSession session;
    private SessionInfo si;

    protected RedisServerConnection(RedisServer server, WritableChannel writableChannel,
            Scheduler scheduler) {
        super(writableChannel, true);
        this.server = server;
        this.scheduler = scheduler;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public SessionInfo getSessionInfo() {
        return si;
    }

    @Override
    public void closeSession(SessionInfo si) {
    }

    @Override
    public int getSessionCount() {
        return 1;
    }

    @Override
    public void close() {
        if (session == null)
            return;
        try {
            session.close();
            super.close();
        } catch (Exception e) {
        }
        session = null;
        si = null;
        server.removeConnection(this);
    }

    @Override
    public int getPacketLength() {
        return 0;
    }

    @Override
    public void handle(NetBuffer buffer) {
    }
}
