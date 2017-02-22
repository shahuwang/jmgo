package com.shahuwang.connection;

/**
 * Created by rickey on 2017/2/21.
 */
@FunctionalInterface
public interface INewDialer {
    IConn dial(ServerAddr addr);
}
