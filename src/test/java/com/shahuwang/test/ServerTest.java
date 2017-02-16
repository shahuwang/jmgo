package com.shahuwang.test;

import com.shahuwang.Server;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by rickey on 2017/2/15.
 */
public class ServerTest {
    @Test
    public void addTest(){
        Server s = new Server();
        int ret = s.add(11, 12);
        assertEquals(23, ret);
        System.out.println("你好");

    }
}
