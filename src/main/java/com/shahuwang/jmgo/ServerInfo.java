package com.shahuwang.jmgo;


import org.bson.BsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rickey on 2017/2/23.
 */
public class ServerInfo {
    private boolean master = false;
    private boolean mongos = false;
    private int maxWireVersion = 0;
    private String setName = "";
    private List<BsonElement> tags;

    public ServerInfo(){
        this.tags = new ArrayList<>(10);
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public boolean isMongos() {
        return mongos;
    }

    public void setMongos(boolean mongos) {
        this.mongos = mongos;
    }
}
