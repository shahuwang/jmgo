package com.shahuwang.jmgo.exceptions;

/**
 * Created by rickey on 2017/2/23.
 */
public class SocketDeadException extends JmgoException{
    private String message;
    public SocketDeadException(String message) {
        this.message = message;
    }
}
