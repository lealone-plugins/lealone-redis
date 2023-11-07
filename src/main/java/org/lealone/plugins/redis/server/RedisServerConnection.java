/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package org.lealone.plugins.redis.server;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Properties;

import org.lealone.common.logging.Logger;
import org.lealone.common.logging.LoggerFactory;
import org.lealone.db.ConnectionInfo;
import org.lealone.db.Constants;
import org.lealone.db.session.ServerSession;
import org.lealone.net.NetBuffer;
import org.lealone.net.WritableChannel;
import org.lealone.plugins.postgresql.server.io.NetBufferOutput;
import org.lealone.server.AsyncServerConnection;
import org.lealone.server.Scheduler;
import org.lealone.server.SessionInfo;

public class RedisServerConnection extends AsyncServerConnection {

    private static final Logger logger = LoggerFactory.getLogger(RedisServerConnection.class);

    private final RedisServer server;
    private final Scheduler scheduler;
    private SocketChannel channel;
    private ServerSession session;
    private SessionInfo si;

    protected RedisServerConnection(RedisServer server, WritableChannel writableChannel,
            Scheduler scheduler) {
        super(writableChannel, true);
        this.server = server;
        this.scheduler = scheduler;
        this.channel = writableChannel.getSocketChannel();
        createSession();
    }

    private void createSession() {
        Properties info = new Properties();
        info.put("USER", "root");
        info.put("PASSWORD", "");
        String url = Constants.URL_PREFIX + Constants.URL_TCP + server.getHost() + ":" + server.getPort()
                + "/redis";
        ConnectionInfo ci = new ConnectionInfo(url, info);
        ci.setRemote(false);
        session = (ServerSession) ci.createSession();
        si = new SessionInfo(scheduler, this, session, -1, -1);
        scheduler.addSessionInfo(si);
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
    public ByteBuffer getPacketLengthByteBuffer() {
        return null; // redis的包没有header，所以返回null，自己解析
    }

    @Override
    public int getPacketLength() {
        return 0;
    }

    @Override
    public void handleException(Exception e) {
        if (endException == e) {
            if (logger.isDebugEnabled())
                logger.debug(endExceptionMsg);
        }
        close();
    }

    public static final byte DOLLAR_BYTE = '$';
    public static final byte ASTERISK_BYTE = '*';
    public static final byte PLUS_BYTE = '+';
    public static final byte MINUS_BYTE = '-';
    public static final byte COLON_BYTE = ':';

    private final ByteBuffer buffer = ByteBuffer.allocate(4096);
    private final EOFException endException = new EOFException();
    private int endOfStreamCount;
    private String endExceptionMsg;

    @Override
    public void handle(NetBuffer netBuffer) {
        try {
            int readBytes = channel.read(buffer);
            if (readBytes > 0) {
                endOfStreamCount = 0;
            } else {
                // 客户端非正常关闭时，可能会触发JDK的bug，导致run方法死循环，selector.select不会阻塞
                // netty框架在下面这个方法的代码中有自己的不同解决方案
                // io.netty.channel.nio.NioEventLoop.processSelectedKey
                if (readBytes < 0) {
                    endOfStreamCount++;
                    if (endOfStreamCount > 3) {
                        endExceptionMsg = "socket channel closed: " + channel.getRemoteAddress();
                        throw endException;
                    }
                }
            }
        } catch (Exception e) {
            handleException(e);
        }

        NetBufferOutput out = new NetBufferOutput(getWritableChannel(), 4096,
                scheduler.getDataBufferFactory());
        out.write(PLUS_BYTE);
        out.write("OK".getBytes());
        out.write('\r');
        out.write('\n');
        out.flush();
    }
}
