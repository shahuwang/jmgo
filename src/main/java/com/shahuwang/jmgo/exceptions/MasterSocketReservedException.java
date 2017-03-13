package com.shahuwang.jmgo.exceptions;

import com.shahuwang.jmgo.exceptions.JmgoException;

/**
 * Created by rickey on 2017/3/13.
 */
public class MasterSocketReservedException extends JmgoException{
    private String message = "SetSocket(master) with existing master socket reserved";
}
