package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.PoolLimitException;
import com.shahuwang.jmgo.exceptions.ServerClosedException;
import com.shahuwang.jmgo.exceptions.SocketDeadException;
import com.shahuwang.jmgo.utils.SyncChan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Duration;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
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
            this.rwlock.readLock().lock();
            MongoSocket socket;
            boolean abended = this.abended;
            if(this.closed){
                this.rwlock.readLock().unlock();
                throw new ServerClosedException();
            }

            int n = this.unusedSockets.size();
            // live - unused 等于当前正在使用的socket数量
            if (poolLimit > 0 && this.liveSockets.size() - n >= poolLimit){
                this.rwlock.readLock().unlock();
                throw new PoolLimitException();
            }
            if (n > 0) {
                socket = this.unusedSockets.get(n - 1);
                this.unusedSockets.remove(n - 1);
                ServerInfo serverInfo = this.serverInfo;
                this.rwlock.readLock().unlock();
                try{
                    socket.initialAcquire(serverInfo, timeout);
                }catch (SocketDeadException e){
                    continue;
                }
            } else {
                this.rwlock.readLock().unlock();
                try{
                    socket = this.connect(timeout);
                    this.rwlock.readLock().lock();
                    if (this.closed) {
                        this.rwlock.readLock().unlock();
                        socket.release();
                        socket.close();
                        throw new ServerClosedException();
                    }
                    this.liveSockets.add(socket);
                    this.rwlock.readLock().unlock();
                }catch (JmgoException e){
                    logger.catching(e);
                }
            }
        }
    }

    public MongoSocket connect(Duration timeout) throws JmgoException{
        //TODO
        return null;
    }

    protected void pinger(boolean loop){

    }

    public boolean isAbended() {
        return abended;
    }
}
