package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.SocketDeadException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.omg.PortableInterceptor.RequestInfo;


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

    public void query(Object ...ops){
        List<Object> nops = Arrays.asList(ops);
        Object[] lops = this.flushLogout();
        if(lops.length > 0){
            nops.addAll(Arrays.asList(lops));
        }
        List<Byte> buf = new ArrayList<>(256);
        List<RequestInfo> requests = new ArrayList<>(nops.size());
        int requestCount = 0;
        for(Object op: ops){

        }
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
        //TODO
    }

    private Object[] flushLogout(){
        //TODO
        return new Object[0];
    }

    private void readLoop() {
        byte[] p = new byte[36];
        byte[] s = new byte[4];
        Socket conn = this.conn;
        while (true){
            try{
                fill(conn, p);
            }catch (IOException e){
                this.kill(new JmgoException(e.getMessage()), true);
                return;
            }
            int totalLen = getInt32(p, 0);
            int responseTo = getInt32(p, 8);
            int opCode = getInt32(p, 12);
            if(opCode != 1){
                this.kill(new JmgoException("opcode != 1, corrupted data?"), true);
                return;
            }
            ReplyOp reply = new ReplyOp(getInt32(p, 16), getInt32(p, 20), getInt32(p, 28), getInt32(p, 32));
            Stats.getInstance().setReceivedOps(1);
            Stats.getInstance().setReceivedDocs(reply.getReplyDocs());
            this.lock.lock();
            IReply replyFunc = this.replyFuncs.get(responseTo);
            if(replyFunc != null){
                this.replyFuncs.remove(responseTo);
            }
            this.lock.unlock();
            if(replyFunc != null && reply.getReplyDocs() == 0){
                replyFunc.reply(null, reply, -1, null);
            }else{
                for(int i=0; i != reply.getReplyDocs(); i++){
                    try {
                        fill(this.conn, s);
                    }catch (IOException e){
                        if(replyFunc != null){
                            replyFunc.reply(new JmgoException(e.getMessage()), null, -1, null);
                        }
                        this.kill(new JmgoException(e.getMessage()), true);
                        return;
                    }
                    byte[] b = new byte[getInt32(s, 0)];
                    b[0] = s[0];
                    b[1] = s[1];
                    b[2] = s[2];
                    b[3] = s[3];
                    byte[] b2 = new byte[b.length - 4];
                    try {
                        fill(this.conn, b2);
                        System.arraycopy(b2, 0, b, 4, b2.length);
                    }catch (IOException e){
                        if(replyFunc != null){
                            replyFunc.reply(new JmgoException(e.getMessage()), null, -1, null);
                            this.kill(new JmgoException(e.getMessage()), true);
                            return;
                        }
                    }
                    if(replyFunc != null){
                        replyFunc.reply(null, reply, i, b);
                    }
                }
            }
            this.lock.lock();
            if(this.replyFuncs.isEmpty()){
                try {
                    this.conn.setSoTimeout(0);
                }catch (SocketException e){
                    logger.catching(e);
                }
            } else {
                //TODO
            }
            this.lock.unlock();
        }
    }

    private static void fill(Socket conn, byte[] b)throws IOException{
        int l = b.length;
        InputStream in = conn.getInputStream();
        int n = in.read(b);
        while (n != l){
            byte[] nibyte = new byte[l-n];
            int ni = in.read(nibyte);
            System.arraycopy(nibyte, 0, b, n, ni);
            n += ni;
        }
    }

    private int getInt32(byte[] b, int pos){
        return b[pos+0] | (b[pos+1] << 8) | (b[pos+2] << 16) | (b[pos+3]<<24);
    }

    private int getInt64(byte[] b, int pos){
        return b[pos+0] |
                (b[pos+1]<<8) |
                (b[pos+2]<<16) |
                (b[pos+3]<<24) |
                (b[pos+4]<<32) |
                (b[pos+5]<<40) |
                (b[pos+6]<<48) |
                (b[pos+7]<<56);
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
