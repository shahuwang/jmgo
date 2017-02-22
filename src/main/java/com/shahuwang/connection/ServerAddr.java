package com.shahuwang.connection;

import java.net.InetSocketAddress;

/**
 * Created by rickey on 2017/2/21.
 */
public class ServerAddr {
    private String addr;
    private InetSocketAddress tcp;

    public ServerAddr(String addr, InetSocketAddress tcp){
        this.addr = addr;
        this.tcp = tcp;
    }

    public String getAddr() {
        return addr;
    }

    public InetSocketAddress getTcp() {
        return tcp;
    }
}
