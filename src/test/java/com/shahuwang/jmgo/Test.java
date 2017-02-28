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
        Properties prop = new Properties();
        try{
            InputStream in = ClassLoader.getSystemResourceAsStream("build.properties");
            prop.load(in);
            String race = prop.getProperty("racedect");
            System.out.println(race);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
