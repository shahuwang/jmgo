package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/2/28.
 */
public enum Mode {
    // 全部从primary里操作
    PRIMARY,
    //从primary里读取，如果不可用，则从Secondary里面读取
    PRIMARY_PREFERRED,
    //从某个secondary里面读取
    SECONDARY,
    //从最近的某个secondary里面读取，如果不可行，从primary里面读取
    SECONDARY_PREFERRED,
    //从最近的一个服务器读取，不管是primary还是secondary
    NEAREST,
    //mgo自己设置的，和NEAREST，但是在读的过程中可能会改变servers
    EVENTULA,
    // 第一次写之前，和SECONDARY_PREFFERED一样，第一次写之后，和Primary一样
    MONOTONIC,
    // 和primary一样，要被mgo v3抛弃了
    STRONG
}
