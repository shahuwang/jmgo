package com.shahuwang.server;

import com.shahuwang.connection.MongoSocket;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by rickey on 2017/2/16.
 */
public class MongoServer {
    private ReadWriteLock rwlock;
    private String addr;
    private InetSocketAddress tcpaddr;
    private Vector<MongoSocket> unusedSockets;
    private Vector<MongoSocket> liveSockets;
    private boolean closed;
    private boolean abended;
    private Duration pingValue;
    private int pingIndex;
    private int pingCount;
    private Duration[] pingWindow = new Duration[6];
    private MongoServerInfo info;
    private Dialer dial;

    private static final Logger logger = LogManager.getLogger(MongoServer.class);

    public MongoServer(String addr, InetSocketAddress tcpaddr, Dialer dial){
        this.addr = addr;
        this.tcpaddr = tcpaddr;
        this.dial = dial;
        this.info = new MongoServerInfo();
        this.pingValue = Duration.ofHours(1);
    }

    public MongoSocket connect(Duration timeout){
        this.rwlock.readLock().lock();
        // 保证master 和 dialer是一致的同步的
        boolean master = this.info.isMaster();
        Dialer dial = this.dial;
        this.rwlock.readLock().unlock();
        logger.info("Establishing new connection to %s (timeout=%d)...", this.addr, timeout.getSeconds());
    }
}
