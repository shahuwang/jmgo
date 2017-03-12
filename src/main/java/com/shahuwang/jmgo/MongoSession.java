package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;

import java.time.Duration;

import java.util.Vector;
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
    private String defaultdb;
    private String sourcedb; //
    private Credential dialCred;
    private Vector<Credential> creds = new Vector<>();
    private int poolLimit;
    private Duration syncTimeout;
    private Duration sockTimeout;
    private QueryConfig queryConfig;

    private final float defaultPrefetch = 0.25f;
    private final int maxUpsertRetries = 5;

    public MongoSession(String url) throws JmgoException{
        DialInfo info = new DialInfo(url);
        info.setTimeout(Duration.ofSeconds(10));
        dialWithInfo(info);
        setSyncTimeout(Duration.ofMinutes(1));
        setSocketTimeout(Duration.ofMinutes(1));
    }

    public MongoSession(String url, Duration timeout) throws JmgoException{
        DialInfo info = new DialInfo(url);
        info.setTimeout(timeout);
        dialWithInfo(info);
    }

    protected MongoSession(Mode consistency, Cluster mcluster, Duration timeout){

    }

    public void setSocket(MongoSocket socket){

    }

    public void close(){

    }

    public void ping() throws JmgoException{

    }

    public void setMode(Mode mode, boolean refresh){

    }

    public void setSyncTimeout(Duration timeout) {
        this.m.writeLock().lock();
        this.syncTimeout = timeout;
        this.m.writeLock().unlock();
    }

    public void setSocketTimeout(Duration timeout){
        this.m.writeLock().lock();
        this.sockTimeout = timeout;
        if(this.masterSocket != null){
            this.masterSocket.setTimeout(timeout);
        }
        if(this.slaveSocket != null){
            this.slaveSocket.setTimeout(timeout);
        }
        this.m.writeLock().unlock();
    }

    public Object Run(Object cmd, Object result){
        return null;
    }

    public void setSafe(Safe safe){

    }

    private MongoSession dialWithInfo(DialInfo info)throws JmgoException {
        ServerAddr[] addrs = new ServerAddr[info.getAddrs().length];
        String [] addrInfo = info.getAddrs();
        for(int i = 0; i<addrs.length; i++){
            String addr = addrInfo[i];
            addrs[i] = new ServerAddr(addr);
        }
        Cluster cluster = new Cluster(addrs, info.isDirect(), info.isFailFast(), new Dialer(), info.getReplicaSetName());
        //MongoSession session = new MongoSession(Mode.EVENTULA, cluster, info.getTimeout());
        this.cluster_ = cluster;
        this.syncTimeout = info.getTimeout();
        this.sockTimeout = info.getTimeout();
        this.poolLimit = 4096;
        this.setMode(Mode.EVENTULA, true);
        this.setSafe(new Safe());
        this.queryConfig.setPrefetch(defaultPrefetch);
        this.defaultdb = info.getDatabase();
        if(this.defaultdb == ""){
            this.defaultdb = "test";
        }
        this.sourcedb = info.getSource();
        if(this.sourcedb == ""){
            this.sourcedb = info.getDatabase();
            if(this.sourcedb == ""){
                this.sourcedb = "admin";
            }
        }
        if(info.getUserName() != ""){
            String source = this.sourcedb;
            // https://docs.mongodb.com/manual/core/authentication/#authentication-mechanisms
            if (info.getSource() == "" &&
                    (info.getMechanism() == "GASSAPI") || info.getMechanism() == "PLAIN" || info.getMechanism() == "MONGODB-X509"){
                source = "$external";
            }
            this.dialCred = new Credential(
                    info.getUserName(), info.getPassword(), info.getMechanism(), info.getService(), info.getServiceHost(), source
            );
            this.creds.add(this.dialCred);
        }
        if (info.getPoolLimit() > 0){
            this.poolLimit = info.getPoolLimit();
        }
        cluster.Release();
        try{
            this.ping();
        }catch (JmgoException e){
            this.close();
            throw new JmgoException(e.getMessage());
        }
        this.setMode(Mode.STRONG, true);
        return this;
    }

    class Dialer implements IDialer{};
}
