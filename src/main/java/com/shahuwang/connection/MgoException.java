package com.shahuwang.connection;

/**
 * Created by rickey on 2017/2/21.
 */
public class MgoException extends Exception {
    private String message;
    public MgoException(String message){
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
