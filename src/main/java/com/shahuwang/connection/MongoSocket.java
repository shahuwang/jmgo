package com.shahuwang.connection;
import com.shahuwang.auth.Credential;
import com.shahuwang.server.MongoServer;
import com.shahuwang.server.MongoServerInfo;
import com.shahuwang.utils.IError;

import java.time.Duration;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by rickey on 2017/2/16.
 */
public class MongoSocket {
    private IConn conn;
    private Duration timeout;
    private String addr; //for debugging only
    private int nextRequestId;
    private HashMap<Integer, IReplyFunc> replyFuncs;
    private int references;
    private Vector<Credential> creds;
    private Vector<Credential> logout;
    private String cacheNonce;
    private IError dead;
    private MongoServerInfo serverInfo;
    private MongoServer server;
    private ReadWriteLock rwlock;

    public MongoSocket(MongoServer server, IConn conn, Duration timeout){
        this.server = server;
        this.conn = conn;
        this.timeout = timeout;
        this.replyFuncs = new HashMap<>();
    }


}
