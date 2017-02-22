package com.shahuwang.connection;

import java.net.SocketAddress;

/**
 * Created by rickey on 2017/2/16.
 */
public interface IConn {
    int read(byte[] b);
    int write(byte[] b);
    void close();
    SocketAddress localAddr();
    SocketAddress RemoteAddr();
}
