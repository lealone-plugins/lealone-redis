/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package com.lealone.plugins.redis.server.handler;

import com.lealone.common.logging.Logger;
import com.lealone.common.logging.LoggerFactory;
import com.lealone.common.util.StringUtils;
import com.lealone.db.Constants;
import com.lealone.storage.Storage;
import com.lealone.storage.StorageMap;

import com.lealone.plugins.redis.server.RedisServer;
import com.lealone.plugins.redis.server.RedisServerConnection;

public class CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);

    private final RedisServerConnection conn;
    private final Storage storage;
    private final StorageMap<Object, Object> redisStorageMap;

    public CommandHandler(RedisServerConnection conn) {
        this.conn = conn;
        storage = conn.getSessionInfo().getSession().getDatabase()
                .getStorage(Constants.DEFAULT_STORAGE_ENGINE_NAME);
        redisStorageMap = storage.openMap("redis", null);
    }

    public void handle(String[] commandArguments) {
        logger.info("Execute command: " + StringUtils.arrayCombine(commandArguments, ' '));
        switch (commandArguments[0]) {
        case "SET":
            redisStorageMap.put(commandArguments[1], commandArguments[2]);
            conn.sendStatusCodeReply();
            break;
        case "GET":
            Object value = redisStorageMap.get(commandArguments[1]);
            conn.sendBulkReply(value);
            break;
        case "COMMAND":
            conn.sendBulkReply("GET");
            break;
        case "INFO":
            conn.sendBulkReply("redis_version " + RedisServer.VERSION);
            break;
        case "QUIT":
            conn.close();
            break;
        default:
            conn.sendError("Unsupported command: " + commandArguments[0]);
        }
    }
}
