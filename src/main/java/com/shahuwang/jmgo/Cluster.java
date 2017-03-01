package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.ReferenceZeroException;
import com.shahuwang.jmgo.utils.SyncChan;

import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by rickey on 2017/3/2.
 */
public class Cluster {
    private ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
    private String[] userSeeds;
    private String[] dynaSeeds;
    private Servers servers;
    private Servers masters;
    private int references;
    private boolean syncing;
    private boolean direct;
    private boolean failFast;
    private int syncCount;
    private String setName;
    private Map<String, Boolean> cachedIndex;
    private SyncChan<Boolean> sync;
    private IDialer dialer;

    Logger logger = LogManager.getLogger(Cluster.class.getName());
    public Cluster(String[] userSeeds, boolean direct, boolean failFast, IDialer dialer, String setName){
        this.userSeeds = userSeeds;
        this.references = 1;
        this.direct = direct;
        this.failFast = failFast;
        this.dialer = dialer;
        this.setName = setName;
        this.sync = new SyncChan<>();
        Stats.getInstance().setCluster(1);
    }

    public void Acquire() {
        this.rwlock.writeLock().lock();
        this.references++;
        logger.info("Cluster {} acquired (refs={})", this, this.references);
        this.rwlock.writeLock().unlock();
    }

    public void Release() throws ReferenceZeroException{
        this.rwlock.writeLock().lock();
        if(this.references == 0){
            this.rwlock.writeLock().unlock();
            throw new ReferenceZeroException("cluster release with references == 0");
        }
        this.references--;
        logger.info("Cluster {} released (refs={})", this, this.references);
        if(this.references == 0) {
            for(MongoServer server: this.servers.getSlice()){
                server.close();
            }
            this.syncServers();
            Stats.getInstance().setCluster(-1);
        }
        this.rwlock.writeLock().unlock();
    }

    public ServerAddr[] liveServers() {
        this.rwlock.readLock().lock();
        ServerAddr [] servers = new ServerAddr[this.servers.len()];
        for(int i=0; i<this.servers.len(); i++){
            MongoServer serv = this.servers.get(i);
            servers[i] = serv.getAddr();
        }
        this.rwlock.readLock().unlock();
        return servers;
    }

    private void removeServer(MongoServer server){
        this.rwlock.writeLock().lock();
        this.masters.remove(server);
        MongoServer other = this.servers.remove(server);
        this.rwlock.writeLock().unlock();
        if(other != null) {
            other.close();
            logger.info("Removed server {} from cluster", server.getAddr().getTcpaddr().toString());
        }
        server.close();
    }

    private void syncServers(){

    }
}
