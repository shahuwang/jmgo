package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.SocketDeadException;

import java.net.Socket;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.conversions.Bson;

/**
 * Created by rickey on 2017/2/23.
 */
public class MongoSocket {
    private ServerInfo serverInfo;
    private Socket conn;
    private MongoServer server;
    private Duration timeout;
    private ServerAddr addr;
    private int nextRequestId;
    private int references;
    private Credential[] creds;
    private Credential[] logout;
    private String cachedNonce;
    private Map<Integer, IReply>replyFuncs;
    private Lock lock = new ReentrantLock();
    private Condition gotNonce;
    private SocketDeadException dead;
    Logger logger = LogManager.getLogger(MongoSocket.class.getName());

    public MongoSocket(MongoServer server, Socket conn, Duration timeout)throws SocketDeadException{
        this.conn = conn;
        this.server = server;
        this.timeout = timeout;
        this.replyFuncs = new HashMap<>();
        this.gotNonce = this.lock.newCondition();
        this.initialAcquire(server.getInfo(), timeout);
        Stats.getInstance().setSocketsAlive(1);
        logger.info("Socket {} to {}: initialized", this, this.addr);

    }

    public void initialAcquire(ServerInfo serverInfo, Duration timeout) throws SocketDeadException{
        this.lock.lock();
        if(this.references > 0){
            throw new SocketDeadException("Socket acquired out of cache with references");
        }
        if(this.dead != null){
            this.lock.unlock();
            throw dead;
        }
        this.references++;
        this.serverInfo = serverInfo;
        this.timeout = timeout;
        Stats.getInstance().setSocketsInUse(1);
        Stats.getInstance().setSocketRefs(1);
        this.lock.unlock();
    }

    public void release(){

    }

    public void close(){

    }

    public byte [] SimpleQuery(QueryOp op){
        return null;
    }

    public void setTimeout(Duration timeout){

    }

    public void login(Credential cred) {

    }

    public void logout(String source){

    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public ServerInfo acquire() {
        return null;
    }

    private void kill(JmgoException error, boolean abend){

    }

    private void resetNonce(){
        logger.debug("Socket {} to {}: requesting a new nonce", this, this.addr);
        QueryOp op = new QueryOp.QueryOpBuilder("admin.$cmd",
                new BsonDocument("getnonce", new BsonInt32(1))).limit(-1).build();
        op.setReplyFuncs((JmgoException dead, ReplyOp reply, int docNum, byte[]docData) -> {
            if(dead != null){
                this.kill(new JmgoException("getNonce: " + dead.getMessage()), true);
                return;
            }
            
        });

    }
}
