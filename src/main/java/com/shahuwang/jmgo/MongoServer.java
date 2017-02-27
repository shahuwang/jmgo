package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.*;
import com.shahuwang.jmgo.utils.SyncChan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;
import java.time.Duration;
import java.util.Vector;
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

    protected void pinger(boolean loop){

    }

    public boolean isAbended() {
        return abended;
    }
}
