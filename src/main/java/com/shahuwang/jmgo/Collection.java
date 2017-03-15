package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import com.shahuwang.jmgo.exceptions.NoReachableServerException;
import com.shahuwang.jmgo.exceptions.SessionClosedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;

import java.util.List;

/**
 * Created by rickey on 2017/3/12.
 */
public class Collection {
    private Database database;
    private String name;
    private String fullName;
    Logger logger = LogManager.getLogger(InsertOp.class.getName());

    public Collection(Database database, String name, String fullName){
        this.database = database;
        this.name = name;
        this.fullName = fullName;
    }

    public void inert(BsonDocument ...docs){

    }


    public Database getDatabase() {
        return database;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    private <T> void writeOp(T op, boolean ordered)throws JmgoException{
        MongoSession session = this.database.getSession();
        MongoSocket socket = null;
        try {
//          Every mongod instance has its own local database,
//          which stores data used in the replication process, and other instance-specific data.
//          The local database is invisible to replication: collections in the local database are not replicated.
            socket = session.acquireSocket(this.database.getName() == "local");
        }catch (SessionClosedException | NoReachableServerException e){
            logger.catching(e);
            throw new JmgoException(e.getMessage());
        }
        session.getM().readLock().lock();
        QueryOp safeOp = session.getSafeOp();
        boolean bypassValidation = session.isBypassValidation();
        session.getM().readLock().unlock();
        if(socket.getServerInfo().getMaxWireVersion() >= 2) {
            if(op instanceof InsertOp && ((InsertOp) op).getDocuments().length > 1000){
                List<BsonDocument> all = ((InsertOp) op).getDocuments();
                for(int i = 0; i<all.size(); i+=1000){
                    int l = i + 1000;
                    if (l > all.size()){
                        l = all.size();
                    }
                    ((InsertOp) op).setDocuments(all.subList(i, l));
                    //TODO
                }
            }
        }
    }

    private <T> void writeOpCommand(MongoSocket socket, QueryOp safaop, T op, boolean ordered, boolean bypasssValidation){
        
    }
}
