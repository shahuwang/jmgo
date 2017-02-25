package com.shahuwang.jmgo.utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by rickey on 2017/2/24.
 * 用于模仿Go的 chan ， 当 capcity 为 1 的情况
 */
public class SyncChan<E> {
    private BlockingQueue<E> queue = new ArrayBlockingQueue<E>(1);
    Logger logger = LogManager.getLogger(SyncChan.class.getName());
    public void put(E e){
        try{
            this.queue.put(e);
        }catch (InterruptedException exception){
            logger.catching(exception);
            return;
        }
    }

    public E take() {
        try{
            return this.queue.take();
        }catch (InterruptedException exception){
            logger.catching(exception);
            return null;
        }
    }
}
