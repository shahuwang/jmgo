package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.SocketDeadException;

import java.net.Socket;
import java.time.Duration;

/**
 * Created by rickey on 2017/2/23.
 */
public class MongoSocket {
    public MongoSocket(MongoServer server, Socket conn, Duration timeout){

    }

    public void initialAcquire(ServerInfo serverInfo, Duration timeout) throws SocketDeadException{

    }

    public void release(){

    }

    public void close(){

    }

    public byte [] SimpleQuery(QueryOp op){
        return null;
    }
}
