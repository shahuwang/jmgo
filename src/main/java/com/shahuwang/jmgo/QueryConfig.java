package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/13.
 */
public class QueryConfig {
    private float prefetch;
    private int limit;
    private QueryOp op;

    public QueryOp getOp() {
        return op;
    }

    public float getPrefetch() {
        return prefetch;
    }

    public void setPrefetch(float prefetch) {
        this.prefetch = prefetch;
    }

    public QueryConfig clone(){
        //TODO
        return null;
    }
}
