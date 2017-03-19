package com.shahuwang.jmgo;

import junit.framework.TestCase;

/**
 * Created by rickey on 2017/3/19.
 */
public class ByteTest extends TestCase{
    private int getInt32(byte[] b, int pos){
        return b[pos+0] | (b[pos+1] << 8) | (b[pos+2] << 16) | (b[pos+3]<<24);
    }

    public void testGetInt(){
        String s = "abcd";
        System.out.println(getInt32(s.getBytes(), 0));
        System.out.println(s instanceof String);
    }
}
