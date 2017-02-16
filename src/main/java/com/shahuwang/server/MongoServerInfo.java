package com.shahuwang.server;

/**
 * Created by rickey on 2017/2/16.
 */
public class MongoServerInfo {
    private boolean master;
    private boolean mongos;
    private int maxWireVersion;
    private String setName;

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public void setMongos(boolean mongos) {
        this.mongos = mongos;
    }

    public boolean isMongos() {
        return mongos;
    }

    public int getMaxWireVersion() {
        return maxWireVersion;
    }

    public void setMaxWireVersion(int maxWireVersion) {
        this.maxWireVersion = maxWireVersion;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }
}
