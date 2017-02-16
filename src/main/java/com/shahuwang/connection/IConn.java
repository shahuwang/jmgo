package com.shahuwang.connection;

/**
 * Created by rickey on 2017/2/16.
 */
public interface IConn {
    int read(byte[] b);
    int write(byte[] b);
    void close();
    IAddr localAddr();
    IAddr RemoteAddr();
}
