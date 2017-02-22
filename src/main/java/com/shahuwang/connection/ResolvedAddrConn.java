package com.shahuwang.connection;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;

/**
 * Created by rickey on 2017/2/21.
 */
public class ResolvedAddrConn implements IConn {
    private String resolvedAddr;
    private Duration timeout;
    private String host;
    private int port;
    private Socket conn;

    public ResolvedAddrConn(String resolvedAddr, Duration timeout){
        this.resolvedAddr = resolvedAddr;
        this.timeout = timeout;
        this.resolve(resolvedAddr);
    }

    public void dial() throws IOException {

        Socket socket = new Socket(this.host, this.port);
        socket.setSoTimeout((int) timeout.getSeconds());
        socket.setKeepAlive(true);
        this.conn = socket;

    }

    private void resolve(String resolvedAddr) {
        String[] sp = resolvedAddr.split(":");
        if(sp.length == 1){
            this.host = resolvedAddr;
            this.port = 80;
        }else {
            this.host = sp[0];
            this.port = Integer.parseInt(sp[1]);
        }
    }

    @Override
    public int read(byte[] b) {
        return 0;
    }

    @Override
    public int write(byte[] b) {
        return 0;
    }

    @Override
    public void close() {

    }

    @Override
    public IAddr localAddr() {
        return null;
    }

    @Override
    public IAddr RemoteAddr() {
        return null;
    }
}
