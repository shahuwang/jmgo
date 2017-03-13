package com.shahuwang.jmgo;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by rickey on 2017/2/27.
 */
public class Test extends TestCase{
    public void testLoadProperties() {
        ServerAddr addr = new ServerAddr("localhost", 27017);
        System.out.println(addr.getTcpaddr());

    }

    class TestCopy{
        public String name;
        public TestCopy(String name){
            this.name = name;
        }
    }
}
