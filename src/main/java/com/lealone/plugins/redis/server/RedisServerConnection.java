/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package com.lealone.plugins.redis.server;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Properties;

import com.lealone.common.logging.Logger;
import com.lealone.common.logging.LoggerFactory;
import com.lealone.db.ConnectionInfo;
import com.lealone.db.Constants;
import com.lealone.db.scheduler.Scheduler;
import com.lealone.db.session.ServerSession;
import com.lealone.net.NetBuffer;
import com.lealone.net.WritableChannel;
import com.lealone.plugins.redis.server.handler.CommandHandler;
import com.lealone.plugins.redis.server.io.NetBufferOutput;
import com.lealone.plugins.redis.server.io.RedisInputStream;
import com.lealone.plugins.redis.server.io.RedisOutputStream;
import com.lealone.server.AsyncServerConnection;
import com.lealone.server.scheduler.SessionInfo;

public class RedisServerConnection extends AsyncServerConnection {

    private static final Logger logger = LoggerFactory.getLogger(RedisServerConnection.class);

    private final RedisServer server;
    private final Scheduler scheduler;
    private final CommandHandler commandHandler;
    private SocketChannel channel;
    private ServerSession session;
    private SessionInfo si;

    protected RedisServerConnection(RedisServer server, WritableChannel writableChannel,
            Scheduler scheduler) {
        super(writableChannel);
        this.server = server;
        this.scheduler = scheduler;
        this.channel = writableChannel.getSocketChannel();
        createSession();
        commandHandler = new CommandHandler(this);
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
        } else {
            sendError(e);
        }
        close();
    }

    public static final byte DOLLAR_BYTE = '$';
    public static final byte ASTERISK_BYTE = '*';
    public static final byte PLUS_BYTE = '+';
    public static final byte MINUS_BYTE = '-';
    public static final byte COLON_BYTE = ':';

    public static final RuntimeException ensureFillException = new RuntimeException();
    private final RedisInputStream in = new RedisInputStream();
    private final ByteBuffer buffer = ByteBuffer.allocate(4096);
    private final EOFException endException = new EOFException();
    private int endOfStreamCount;
    private String endExceptionMsg;

    @Override
    public void handle(NetBuffer netBuffer) {
        try {
            int readBytes = channel.read(buffer);
            if (readBytes > 0) {
                buffer.flip();
                in.setBuf(buffer.array(), buffer.limit());
                handle();
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
            if (e == ensureFillException)
                return;
            handleException(e);
            return;
        }
    }

    private void handle() {
        final byte b = in.readByte();
        switch (b) {
        case PLUS_BYTE:
        case DOLLAR_BYTE:
        case COLON_BYTE:
        case MINUS_BYTE:
            sendStatusCodeReply();
            break;
        case ASTERISK_BYTE:
            handleCommand();
            break;
        default:
            sendError("Unknown request: " + (char) b);
        }
    }

    private void handleCommand() {
        int size = in.readIntCrLf();
        String[] commandArguments = new String[size];
        for (int i = 0; i < size; i++) {
            in.readLine();
            commandArguments[i] = in.readLine();
        }
        commandHandler.handle(commandArguments);
    }

    public void sendError(Exception e) {
        sendError(e.getMessage());
    }

    public void sendError(String message) {
        RedisOutputStream out = createOut();
        out.write(MINUS_BYTE);
        out.write(message.getBytes());
        out.write('\r');
        out.write('\n');
        out.flush();
    }

    public void sendStatusCodeReply() {
        RedisOutputStream out = createOut();
        out.write(PLUS_BYTE);
        out.write("OK".getBytes());
        out.writeCrLf();
        out.flush();
    }

    public void sendBulkReply(Object value) {
        RedisOutputStream out = createOut();
        out.write(DOLLAR_BYTE);
        byte[] bytes = value.toString().getBytes();
        out.writeIntCrLf(bytes.length);
        out.write(bytes);
        out.writeCrLf();
        out.flush();
    }

    private RedisOutputStream createOut() {
        return new RedisOutputStream(new NetBufferOutput(getWritableChannel(),
                RedisOutputStream.OUTPUT_BUFFER_SIZE, scheduler.getDataBufferFactory()));
    }
}
