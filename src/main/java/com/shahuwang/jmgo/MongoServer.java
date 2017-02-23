package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.PoolLimitException;
import com.shahuwang.jmgo.exceptions.ServerClosedException;
import com.shahuwang.jmgo.exceptions.SocketDeadException;

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
    private BlockingQueue<Boolean> sync;
    private Vector<MongoSocket> unusedSockets = new Vector<>();
    private Vector<MongoSocket> liveSockets = new Vector<>();

    public MongoServer(ServerAddr addr, BlockingQueue<Boolean> sync, IDialer dialer){
        this.addr = addr;
        this.sync = sync;
        this.dialer = dialer;
        this.pingValue = Duration.ofHours(1);
        Thread t = new Thread(() -> {
            pinger(true);
        });
        t.start();
    }

    public MongoSocket AcquireSocket(int poolLimit, Duration timeout)throws PoolLimitException, ServerClosedException{
        while (true){
            this.rwlock.readLock().lock();
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
                MongoSocket mso = this.unusedSockets.get(n - 1);
                this.unusedSockets.remove(n - 1);
                ServerInfo serverInfo = this.serverInfo;
                this.rwlock.readLock().unlock();
                try{
                    mso.InitialAcquire(serverInfo, timeout);
                }catch (SocketDeadException e){
                    continue;
                }
            } else {
                this.rwlock.readLock().unlock();
            }
        }
    }

    protected void pinger(boolean loop){

    }

    public boolean isAbended() {
        return abended;
    }
}
