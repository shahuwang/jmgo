package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.ReferenceZeroException;
import com.shahuwang.jmgo.exceptions.SyncServerException;
import com.shahuwang.jmgo.utils.SyncChan;

import java.time.Duration;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

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
    private Condition serverSynced;
    private String setName;
    private Map<String, Boolean> cachedIndex;
    private SyncChan<Boolean> sync;
    private IDialer dialer;

    protected final static Duration syncSocketTimeout = Duration.ofSeconds(5);

    Logger logger = LogManager.getLogger(Cluster.class.getName());
    public Cluster(String[] userSeeds, boolean direct, boolean failFast, IDialer dialer, String setName){
        this.userSeeds = userSeeds;
        this.references = 1;
        this.direct = direct;
        this.failFast = failFast;
        this.dialer = dialer;
        this.setName = setName;
        this.sync = new SyncChan<>();
        this.serverSynced = this.rwlock.readLock().newCondition();
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

    public MasterAck isMaster(MongoSocket socket) throws JmgoException{
        //TODO
        return null;
    }


    private void syncServers(){
        this.sync.offer(true);
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


    private TopologyInfo syncServer(MongoServer server) throws SyncServerException{
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
                result = this.isMaster(socket);
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
        List<String> hosts = new ArrayList<>();
        if (result.getPrimary() != "") {
            hosts.add(result.getPrimary());
        }
        for(String h: result.getHosts()){
            hosts.add(h);
        }
        for(String p: result.getPassives()){
            hosts.add(p);
        }

        logger.info("SYNC {} knows about the following peers: {}", addr.getTcpaddr().toString(), hosts);
        return new TopologyInfo(info, hosts.toArray(new String[hosts.size()]));
    }

    private void addServer(MongoServer server, ServerInfo info, boolean syncKind)throws JmgoException{
        this.rwlock.writeLock().lock();
        MongoServer current = this.servers.search(server.getAddr());
        if(current == null){
            if(!syncKind){
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

    private String[] getKnowAddrs(){
        this.rwlock.readLock().lock();
        int max = this.userSeeds.length + this.dynaSeeds.length + this.servers.len();
        Map<String, Boolean> seen = new HashMap<>(max);
        Vector<String> known = new Vector<>(max);
        Consumer<String> add = (String addr) -> {
          if(!seen.containsKey(addr)){
              seen.put(addr, true);
              known.add(addr);
          }
        };
        for(String addr: this.userSeeds){
            add.accept(addr);
        }
        for(String addr: this.dynaSeeds){
            add.accept(addr);
        }
        for(MongoServer serv: this.servers.getSlice()){
            add.accept(serv.getAddr().toString());
        }
        this.rwlock.readLock().unlock();
        return known.toArray(new String[known.size()]);
    }

    private void syncServerLoop(){
        
    }
}
