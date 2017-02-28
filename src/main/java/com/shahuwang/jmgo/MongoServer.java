package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.*;
import com.shahuwang.jmgo.utils.SyncChan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonElement;
import org.bson.BsonInt32;

import java.net.Socket;
import java.time.Duration;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by rickey on 2017/2/23.
 */
public class MongoServer {
    private ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private ServerAddr addr;
    private boolean closed = false;
    private boolean abended = false;
    private IDialer dialer;
    private Duration pingValue;
    private int pingIndex;
    private int pingCount;
    private ServerInfo serverInfo = new ServerInfo();
    private Duration [] pingWindow = new Duration[6];
    // 主要用来通知cluster，此server已经挂掉了
    private SyncChan<Boolean> sync;
    private Vector<MongoSocket> unusedSockets = new Vector<>();
    private Vector<MongoSocket> liveSockets = new Vector<>();

    public static final Duration pingDelay = Duration.ofSeconds(15);

    Logger logger = LogManager.getLogger(MongoServer.class.getName());
    public MongoServer(ServerAddr addr, SyncChan<Boolean> sync, IDialer dialer){
        this.addr = addr;
        this.sync = sync;
        this.dialer = dialer;
        this.pingValue = Duration.ofHours(1);
        Thread t = new Thread(() -> {
            pinger(true);
        });
        t.start();
    }

    public MongoSocket acquireSocket(int poolLimit, Duration timeout)throws PoolLimitException, ServerClosedException{
        while (true){
            this.rwlock.writeLock().lock();
            MongoSocket socket;
            boolean abended = this.abended;
            if(this.closed){
                this.rwlock.writeLock().unlock();
                throw new ServerClosedException();
            }

            int n = this.unusedSockets.size();
            // live - unused 等于当前正在使用的socket数量
            if (poolLimit > 0 && this.liveSockets.size() - n >= poolLimit){
                this.rwlock.writeLock().unlock();
                throw new PoolLimitException();
            }
            if (n > 0) {
                socket = this.unusedSockets.get(n - 1);
                this.unusedSockets.remove(n - 1);
                ServerInfo serverInfo = this.serverInfo;
                this.rwlock.writeLock().unlock();
                try{
                    socket.initialAcquire(serverInfo, timeout);
                }catch (SocketDeadException e){
                    continue;
                }
            } else {
                this.rwlock.writeLock().unlock();
                try{
                    socket = this.connect(timeout);
                    this.rwlock.writeLock().lock();
                    if (this.closed) {
                        this.rwlock.writeLock().unlock();
                        socket.release();
                        socket.close();
                        throw new ServerClosedException();
                    }
                    this.liveSockets.add(socket);
                    this.rwlock.writeLock().unlock();
                }catch (JmgoException e){
                    logger.catching(e);
                }
            }
            return null;
        }
    }

    public MongoSocket connect(Duration timeout)throws  ConnectionException{
        this.rwlock.readLock().lock();
        boolean master = this.serverInfo.isMaster();
        IDialer dialer = this.dialer;
        this.rwlock.readLock().unlock();
        logger.info("Establishing new connection to {} (timeout={})...", this.addr.getTcpaddr(), timeout);
        Socket conn;
        try{
            conn = dialer.dial(this.addr, timeout);
        }catch (JmgoException e){
            logger.error("Connection to {} failed: {}", this.addr.getTcpaddr(), e);
            throw new ConnectionException(e.getMessage());
        }
        logger.info("Connection to {} established.", this.addr.getTcpaddr());
        Stats.getInstance().setConn(1, master);
        return new MongoSocket(this, conn, timeout);
    }

    public void close() {
        this.rwlock.writeLock().lock();
        this.closed = true;
        Vector<MongoSocket> liveSocket = this.liveSockets;
        Vector<MongoSocket> unusedSockets = this.unusedSockets;
        this.rwlock.writeLock().unlock();
        logger.info("Connections to {} closing ({} live sockets).", this.addr.getTcpaddr(), liveSocket.size());
        for(MongoSocket socket: liveSocket){
            socket.close();
        }
        liveSocket.clear();
        unusedSockets.clear();

    }

    public void recycleSocket(MongoSocket socket){
        this.rwlock.writeLock().lock();
        if (!this.closed){
            this.unusedSockets.add(socket);
        }
        this.rwlock.writeLock().unlock();
    }


    public void abendSocket(MongoSocket socket) {
        this.rwlock.writeLock().lock();
        this.abended = true;
        if (this.closed) {
            this.rwlock.writeLock().unlock();
            return;
        }
        this.liveSockets.remove(socket);
        this.unusedSockets.remove(socket);
        this.rwlock.writeLock().unlock();
        this.sync.offer(true);
    }

    public void setInfo(ServerInfo info){
        this.rwlock.writeLock().lock();
        this.serverInfo = info;
        this.rwlock.writeLock().unlock();

    }

    public ServerInfo getInfo() {
        this.rwlock.writeLock().lock();
        ServerInfo info = this.serverInfo;
        this.rwlock.writeLock().unlock();
        return info;
    }

    protected boolean hasTags(BsonElement [][] serverTags){
        nextTagSet:
        for(BsonElement [] tags: serverTags){
            nextReqTag:
            for(BsonElement req: tags){
                for(BsonElement has: this.serverInfo.getTags()){
                    if (req.getName() == has.getName()){
                        if (req.getValue() == has.getValue()){
                            continue nextReqTag;
                        }
                        continue nextTagSet;
                    }
                }
                continue nextTagSet;
            }
            return true;
        }
        return false;
    }

    protected void pinger(boolean loop){
        Duration delay;
        boolean racedetector = BuildConfig.getInstance().getRacedetector();
        if (racedetector) {
            synchronized (GlobalMutex.class){
                delay = this.pingDelay;
            }
        }else {
            delay = this.pingDelay;
        }
        BsonElement [] query = {new BsonElement("ping", new BsonInt32(1)),};
        QueryOp op = new QueryOp.QueryOpBuilder("admin.$cmd", query).flags(QueryOpFlags.FLAG_SLAVE_OK).limit(-1).build();
        while (true){
            if (loop) {
                try {
                    TimeUnit.SECONDS.sleep(delay.getSeconds());
                }catch (InterruptedException e){
                    logger.error(e.getMessage());
                }
            }
            MongoSocket socket;
            try{

                socket = this.acquireSocket(0, delay);
                long start = System.currentTimeMillis();
                socket.SimpleQuery(op);
                long end = System.currentTimeMillis();
                Duration delay2 = Duration.ofMillis(end - start);
                this.pingWindow[this.pingIndex] = delay2;
                this.pingIndex = (this.pingIndex + 1) % this.pingWindow.length;
                this.pingCount ++;
                Duration max = Duration.ofSeconds(0) ;
                for(int i = 0; i < this.pingWindow.length && i < this.pingCount; i++){
                    if (this.pingWindow[i].compareTo(max) > 0){
                        max = this.pingWindow[i];
                    }
                }
                socket.release();
                this.rwlock.writeLock().lock();
                if (this.closed) {
                    loop = false;
                }
                this.pingValue = max;
                this.rwlock.writeLock().unlock();
                logger.info("Ping for {} is {} ms", this.addr.getTcpaddr().getHostString(), max.toMillis());
            }catch (ServerClosedException e){
                return;
            }catch (PoolLimitException e){
                logger.catching(e);
            }
            if (!loop){
                return;
            }
        }
    }

    public boolean isAbended() {
        return abended;
    }
}
