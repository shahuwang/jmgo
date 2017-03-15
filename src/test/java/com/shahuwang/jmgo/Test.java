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
        TestCopy tp = new TestCopy(new TestCopy2("shahuwang"), "tpname=====");
        TestCopy tp2 = tp.clone();
        tp.t.name = "wangruiqi";
        tp.tpname = "tpname++++";
        System.out.println(tp2.t.name);
        System.out.println(tp2.tpname);
        testGeneric(tp);
    }

    private <T>void testGeneric(T t){
      if(t instanceof TestCopy){
          System.out.println("=======");
      }
    }

    class TestCopy{
        public TestCopy2 t;
        public String tpname;
        public TestCopy(TestCopy2 t, String tpname){
            this.t = t;
            this.tpname = tpname;
        }
        public TestCopy clone() {
            return new TestCopy(this.t, this.tpname);
        }
    }

    class TestCopy2{
        public String name;
        public TestCopy2(String name) {
            this.name = name;
        }
    }
}
