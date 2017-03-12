package com.shahuwang.jmgo.exceptions;

/**
 * Created by rickey on 2017/3/12.
 */
public class NoReachableServerException extends JmgoException{
    private String message = "No reachable servers";

    @Override
    public String getMessage() {
        return message;
    }
}
