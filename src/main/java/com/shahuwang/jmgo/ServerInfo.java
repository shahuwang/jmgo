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
    private BsonElement[] tags;


    public ServerInfo(boolean master, boolean mongos, BsonElement[] tags, String setName, int maxWireVersion){
        this.master = master;
        this.mongos = mongos;
        this.tags = tags;
        this.setName = setName;
        this.maxWireVersion = maxWireVersion;
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

    public int getMaxWireVersion() {
        return maxWireVersion;
    }

    public BsonElement[] getTags() {
        return tags;
    }
}
