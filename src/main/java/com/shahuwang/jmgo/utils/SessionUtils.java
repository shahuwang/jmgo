package com.shahuwang.jmgo.utils;

import com.shahuwang.jmgo.Cluster;
import com.shahuwang.jmgo.Credential;
import com.shahuwang.jmgo.MongoSession;
import com.shahuwang.jmgo.exceptions.SessionClosedException;

import java.util.Vector;

/**
 * Created by rickey on 2017/3/13.
 */
public class SessionUtils {
    public MongoSession copySession(MongoSession session, boolean keepCreds)throws SessionClosedException{
        Cluster cluster = session.cluster();
        cluster.Acquire(); // 增加cluster的引用计数
        if(session.getMasterSocket() != null) {
            session.getMasterSocket().acquire(); // 增加socket的引用计数
        }
        if(session.getSlaveSocket() != null) {
            session.getSlaveSocket().acquire();
        }
        Vector<Credential> creds = new Vector<>();
        if (keepCreds) {
            for(Credential c: session.getCreds()) {
                creds.add(c.clone());
            }
        } else if (session.getDialCred() != null) {
            creds.add(session.getDialCred().clone());
        }
        return session;
    }
}
