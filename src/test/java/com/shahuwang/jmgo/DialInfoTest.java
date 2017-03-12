package com.shahuwang.jmgo;

import com.shahuwang.jmgo.exceptions.JmgoException;
import junit.framework.TestCase;

/**
 * Created by rickey on 2017/3/12.
 */
public class DialInfoTest extends TestCase{
    public void testDialInfo() {
        String s = "mongodb://myuser:mypass@localhost:40001,otherhost:40001/mydb";
        try {
            DialInfo info = new DialInfo(s);
            assertEquals(info.getDatabase(), "mydb");
            assertEquals(info.getPassword(), "mypass");
        }catch (JmgoException e){
            System.out.println(e.getMessage());
        }
    }
}
