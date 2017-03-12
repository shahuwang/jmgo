package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.NoReachableServerException;
import com.shahuwang.jmgo.exceptions.ReferenceZeroException;
import com.shahuwang.jmgo.exceptions.SyncServerException;
import com.shahuwang.jmgo.utils.SyncChan;

import java.time.Duration;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonElement;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;


/**
 * Created by rickey on 2017/3/2.
 */
public class Cluster {
    private ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
    private ServerAddr[] userSeeds;
    private ServerAddr[] dynaSeeds;
    private Servers servers;
    private Servers masters;
    private int references;
    private boolean syncing;
    private boolean direct;
    private boolean failFast;
    private int syncCount;
    private Condition serverSynced;
    private String setName;
    private Map<String, Boolean> cachedIndex;
    private SyncChan<Boolean> sync;
    private IDialer dialer;

    protected final static Duration syncSocketTimeout = Duration.ofSeconds(5);
    protected final static Duration syncShortDelay = Duration.ofMillis(500);
    protected final static Duration syncServersDelay = Duration.ofSeconds(30);
    protected final static boolean partialSync = true;
    protected final static boolean completeSync = true;

    Logger logger = LogManager.getLogger(Cluster.class.getName());
    public Cluster(ServerAddr[] userSeeds, boolean direct, boolean failFast, IDialer dialer, String setName){
        this.userSeeds = userSeeds;
        this.references = 1;
        this.direct = direct;
        this.failFast = failFast;
        this.dialer = dialer;
        this.setName = setName;
        this.sync = new SyncChan<>();
        this.serverSynced = this.rwlock.readLock().newCondition();
        Stats.getInstance().setCluster(1);
        Runnable task = () -> {
            this.syncServerLoop();
        };
        Thread thread = new Thread(task);
        thread.start();
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

    public MongoSocket acquireSocket(Mode mode, boolean slaveOk, Duration syncTimeout
            , Duration socketTimeout, BsonElement[][]serverTags, int poolLimit)throws NoReachableServerException{
        Duration started = Duration.ZERO;
        int syncCount = 0;
        boolean warnedLimit = false;
        while (true) {
            this.rwlock.readLock().lock();
            while (true) {
                int masterLen = this.masters.len();
                int slaveLen = this.servers.len() - masterLen;
                logger.debug("Cluster has {} known masters and {} known slaves", masterLen, slaveLen);
                if (masterLen > 0 && !(slaveOk && mode == Mode.SECONDARY) || slaveLen > 0 && slaveOk){
                    break;
                }
                if (masterLen > 0 && mode == Mode.SECONDARY && this.masters.hasMongos()) {
                    break;
                }
                if (started.isZero()) {
                    started = Duration.ofMillis(System.currentTimeMillis());
                    syncCount = this.syncCount;
                } else if (!syncTimeout.isZero() && started.getSeconds() * 1000 < (System.currentTimeMillis() - syncCount) || this.failFast && this.syncCount != syncCount){
                    this.rwlock.readLock().unlock();
                    throw new NoReachableServerException();
                }
                logger.info("Waiting for servers to synchronize...");
                this.syncServers();
                try {

                    this.serverSynced.wait();
                }catch (InterruptedException e){
                    logger.catching(e);
                }
            }

            MongoServer server = null;
            if(slaveOk) {
                server = this.servers.bestFit(mode, serverTags);
            } else {
                server = this.masters.bestFit(mode, serverTags);
            }
            this.rwlock.readLock().unlock();
            if (server == null) {
                try {
                    Thread.sleep(10000);
                }catch (InterruptedException e){
                    logger.catching(e);
                }
                continue;
            }
            //server.acquireSocket();
        }
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

    public void isMaster(MongoSocket socket, MasterAck result) throws JmgoException{
        MongoSession session = new MongoSession(Mode.MONOTONIC, this, Duration.ofSeconds(10));
        session.setSocket(socket);
        session.Run("ismaster", result);
        session.close();
    }


    private void syncServers(){
        this.sync.offer(true);
    }

    protected void removeServer(MongoServer server){
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


    protected TopologyInfo syncServer(MongoServer server) throws SyncServerException{
        Duration syncTimeout = null;
        if (BuildConfig.getInstance().getRacedetector()){
            synchronized (GlobalMutex.class){
                syncTimeout = Cluster.syncSocketTimeout;
            }
        } else {
            syncTimeout = Cluster.syncSocketTimeout;
        }

        ServerAddr addr = server.getAddr();
        logger.info("SYNC Processing {} ...", addr.getTcpaddr());
        MasterAck result = null;
        SyncServerException err = null;
        int retry = -1;
        while (true) {
            retry = retry + 1;
            if(retry == 3 || retry == 1 && this.failFast){
                throw err;
            }
            if (retry > 0) {
                // TODO
                // 这里可能会出现possibleTimeout的错误，但是找遍mgo的代码也没找到这个error的定义是哪里
            }
            MongoSocket socket = null;
            try {
                 socket = server.acquireSocket(0, syncTimeout);
            }catch (JmgoException e){
                String errmsg = String.format("SYNC failed to get socket to %s: %s", addr.getTcpaddr().toString(), e.getMessage());
                err = new SyncServerException(errmsg);
                continue;
            }
            try {
                this.isMaster(socket, result);
                socket.release();
            }catch (JmgoException e){
                socket.release();
                String errmsg = String.format("SYNC Command 'ismaster' to %s failed: %s", addr.getTcpaddr().toString(), e.getMessage());
                err = new SyncServerException(errmsg);
                continue;
            }
            logger.info("SYNC Result of 'ismaster' from %s: %s", addr.getTcpaddr().toString(), result.toString());
            break;
        }
        if(this.setName != "" && result.getSetName() != this.setName){
            String msg = String.format("SYNC Server %s is not a member of replica set %s", addr.getTcpaddr().toString(), this.setName);
            logger.info(msg);
            throw new SyncServerException(msg);
        }
        if (result.isMaster()){
            logger.info("SYNC {} is a master", addr.getTcpaddr().toString());
            if(!server.getInfo().isMaster()){
                Stats.getInstance().setConn(-1, false);
                Stats.getInstance().setConn(1, true);
            }
        }else if(result.isSecondary()){
            logger.info("SYNC {} is a slave", addr.getTcpaddr().toString());
        }else{
            logger.info("SYNC {} is neither a master nor a slave.", addr.getTcpaddr().toString());
            throw new SyncServerException(String.format("SYNC %s is not a master nor slave", addr.getTcpaddr().toString()));
        }
        ServerInfo info = new ServerInfo(result.isMaster(), result.getMsg() == "isdbgrid", result.getTags(), result.getSetName(), result.getMaxWireVersion());
        List<ServerAddr> hosts = new ArrayList<>();
        if (result.getPrimary() != null) {
            hosts.add(result.getPrimary());
        }
        for(ServerAddr h: result.getHosts()){
            hosts.add(h);
        }
        for(ServerAddr p: result.getPassives()){
            hosts.add(p);
        }

        logger.info("SYNC {} knows about the following peers: {}", addr.getTcpaddr().toString(), hosts);
        return new TopologyInfo(info, hosts.toArray(new ServerAddr[hosts.size()]));
    }

    protected void addServer(MongoServer server, ServerInfo info, SyncKind syncKind)throws JmgoException{
        this.rwlock.writeLock().lock();
        MongoServer current = this.servers.search(server.getAddr());
        if(current == null){
            if(syncKind == SyncKind.PARTIAL_SYNC){
                //partialSync
                this.rwlock.writeLock().unlock();
                server.close();
                logger.info("SYNC Discarding unknown server {} due to partial sync", server.getAddr().getTcpaddr().toString());
                return;
            }
            this.servers.add(server);
            if(info.isMaster()){
                this.masters.add(server);
                logger.info("SYNC adding {} to cluster as a master", server.getAddr().getTcpaddr().toString());
            }else{
                logger.info("SYNC adding {} to cluster as a slave", server.getAddr().getTcpaddr().toString());
            }
        }else {
            if(server != current){
                throw new JmgoException("addServer attempting to add duplicated server");
            }
            if(server.getInfo().isMaster() != info.isMaster()){
                if(info.isMaster()){
                    logger.info("SYNC server {} is now a master", server.getAddr().getTcpaddr().toString());
                    this.masters.add(server);
                }else{
                    logger.info("SYNC server {} is now a slave", server.getAddr().getTcpaddr().toString());
                    this.masters.remove(server);
                }
            }
        }
        server.setInfo(info);
        logger.info("SYNC Broadcasting availability of server {}", server.getAddr().getTcpaddr().toString());
        this.serverSynced.signalAll();
        this.rwlock.writeLock().unlock();
    }

    private ServerAddr[] getKnowAddrs(){
        this.rwlock.readLock().lock();
        int max = this.userSeeds.length + this.dynaSeeds.length + this.servers.len();
        Map<ServerAddr, Boolean> seen = new HashMap<>(max);
        Vector<ServerAddr> known = new Vector<>(max);
        Consumer<ServerAddr> add = (ServerAddr addr) -> {
          if(!seen.containsKey(addr)){
              seen.put(addr, true);
              known.add(addr);
          }
        };
        for(ServerAddr addr: this.userSeeds){
            add.accept(addr);
        }
        for(ServerAddr addr: this.dynaSeeds){
            add.accept(addr);
        }
        for(MongoServer serv: this.servers.getSlice()){
            add.accept(serv.getAddr());
        }
        this.rwlock.readLock().unlock();
        return known.toArray(new ServerAddr[known.size()]);
    }


    private void syncServerLoop(){
        while (true){
            logger.debug("SYNC cluster {} is starting a sync loop literation", this);
            this.rwlock.writeLock().lock();
            if (this.references == 0) {
                this.rwlock.writeLock().unlock();
                break;
            }
            this.references++;
            boolean direct = this.direct;
            this.rwlock.writeLock().unlock();
            this.syncServersIteration(direct);
            this.sync.poll();
            try{
                this.Release();
            }catch (ReferenceZeroException e){
                logger.catching(e);
            }
            if (!this.failFast){
                try{
                    Thread.sleep(this.syncShortDelay.toMillis());
                }catch (InterruptedException e){
                    logger.catching(e);
                }
            }
            this.rwlock.writeLock().lock();
            if(this.references == 0) {
                this.rwlock.writeLock().unlock();
                break;
            }
            this.syncCount++;
            this.serverSynced.notifyAll();
            boolean restart = !direct && this.masters.empty() || this.servers.empty();
            this.rwlock.writeLock().unlock();
            if(restart) {
                logger.debug("SYNC no masters found. Will synchronize again.");
                try {

                    Thread.sleep(this.syncShortDelay.toMillis());
                }catch (InterruptedException e){
                    logger.catching(e);
                }
                continue;
            }

            logger.debug("SYNC cluster {} waiting for next requested or scheduled sync. ", this);
            this.sync.pollTimeout(this.syncServersDelay.toMillis(), TimeUnit.MILLISECONDS);
        }
        logger.debug("SYNC cluster {} is stopping its sync loop.", this);
    }

    protected MongoServer server(ServerAddr addr){
        this.rwlock.readLock().lock();
        MongoServer server= this.servers.search(addr);
        if (server != null) {
            return server;
        }
        return new MongoServer(addr, this.sync, this.dialer);
    }

    private void syncServersIteration(boolean direct){
        logger.info("SYNC starting full topology synchronization");
        ServerAddr[] knownAddr = this.getKnowAddrs();
        Phaser phaser = new Phaser();
        for(ServerAddr addr: knownAddr){
            Thread thread = new Thread(new SpawnSync(this, addr, false, direct, phaser));
            thread.start();
        }
        try {
            phaser.wait();
        }catch (InterruptedException e){
            logger.catching(e);
        }
        if (SpawnSync.syncKind == SyncKind.COMPLETE_SYNC) {
            logger.info("SYNC synchronization was complte (got data from primary)");
            for(PendingAdd pending: SpawnSync.notYetAdd.values()){
                this.removeServer(pending.server);
            }
        }else{
            logger.info("SYNC Synchronization was partial (cannot talk to primary).");
            for(PendingAdd pending: SpawnSync.notYetAdd.values()){
                try {
                    this.addServer(pending.server, pending.info, SyncKind.PARTIAL_SYNC);
                }catch (JmgoException e){
                    logger.catching(e);
                }
            }
        }
        this.rwlock.writeLock().lock();
        int masterlen = this.masters.len();
        logger.info("SYNC synchronization completed: {} master(s) and {} slave(s) alive", masterlen, this.servers.len() - masterlen);
        if(SpawnSync.syncKind == SyncKind.COMPLETE_SYNC) {
            ServerAddr[] dynaSeeds = new ServerAddr[this.servers.len()];
            int i = 0;
            for(MongoServer server: this.servers.getSlice()){
                dynaSeeds[i] = server.getAddr();
                i++;
            }
            this.dynaSeeds = dynaSeeds;
            logger.debug("SYNC new dynamic seeds: {}", dynaSeeds);
        }
        this.rwlock.writeLock().unlock();
    }

}
