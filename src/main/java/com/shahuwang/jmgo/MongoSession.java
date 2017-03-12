package com.shahuwang.jmgo;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by rickey on 2017/3/2.
 */
public class MongoSession {
    private ReentrantReadWriteLock m = new ReentrantReadWriteLock();
    private Cluster cluster_;
    private MongoSocket masterSocket;
    private MongoSocket slaveSocket;
    private boolean slaveOk;
    private Mode consistency;

    public MongoSession(Mode consistency, Cluster mcluster, Duration timeout){

    }

    public void setSocket(MongoSocket socket){

    }

    public void close(){

    }

    public Object Run(Object cmd, Object result){
        return null;
    }

}
