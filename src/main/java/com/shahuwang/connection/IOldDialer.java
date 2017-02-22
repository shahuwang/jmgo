package com.shahuwang.connection;

import java.net.SocketAddress;

/**
 * Created by rickey on 2017/2/21.
 */
@FunctionalInterface
public interface IOldDialer {
    IConn dial(SocketAddress addr);
}
