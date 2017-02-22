package com.shahuwang.connection;

/**
 * Created by rickey on 2017/2/16.
 */
public class Dialer {
    private IOldDialer old;
    private INewDialer _new;

    public Dialer(){

    }

    public Dialer(IOldDialer old){
        this.old = old;
    }

    public Dialer(INewDialer _new){
        this._new = _new;
    }

    public Dialer(IOldDialer old, INewDialer _new){
        this.old = old;
        this._new = _new;
    }

    public IOldDialer getOld() {
        return old;
    }

    public INewDialer getNew() {
        return _new;
    }

    public boolean isSet() {
        return this.old != null || this._new != null;
    }
}
