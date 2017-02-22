package com.shahuwang.server;

/**
 * Created by rickey on 2017/2/21.
 */
public class Stats {
    private static Stats ourInstance = new Stats();

    public static Stats getInstance() {
        return ourInstance;
    }

    private Stats() {
    }

    public void conn(int delta, boolean master){

    }
}
