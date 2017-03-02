package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/2.
 */
public class TopologyInfo {
    private ServerInfo info;
    private String[] hosts;

    public TopologyInfo(ServerInfo info, String[] hosts){
        this.info = info;
        this.hosts = hosts;
    }
}
