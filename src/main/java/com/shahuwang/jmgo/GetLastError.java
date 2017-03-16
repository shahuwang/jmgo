package com.shahuwang.jmgo;

import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;

/**
 * Created by rickey on 2017/3/16.
 */

//https://docs.mongodb.com/manual/reference/command/getLastError/
public class GetLastError<T> {
    private int cmdName;
    private T w; // 文档上说可以是数字，也可以是字符串
    private int WTimeout;
    private boolean fsync;
    private boolean j;

    public GetLastError(int cmdName, T w, int WTimeout, boolean j){
        this.cmdName = cmdName;
        this.w = w;
        this.WTimeout = WTimeout;
        this.j = j;

    }

    public BsonDocument encode() {
        BsonDocument doc = new BsonDocument();
        if(this.cmdName != 0) {
            doc.append("getLastError", new BsonInt32(this.cmdName));
        }
        if(this.WTimeout != 0){
            doc.append("wtimeout", new BsonInt32(this.WTimeout));
        }
        if(this.fsync){
            doc.append("fsync", new BsonBoolean(this.fsync));
        }
        if(this.j){
            doc.append("j", new BsonBoolean(this.j));
        }
        if(this.w instanceof Integer){
            if(((Integer) this.w).intValue() != 0){
                doc.append("w", new BsonInt32(((Integer) this.w).intValue()));
            }
        }
        if(this.w instanceof String){
            if(this.w != "" || this.w != null){
                doc.append("w", new BsonString((String)this.w));
            }
        }
        return doc;
    }

    public static class Builder<T>{
        private int cmdName;
        private T w;
        private int WTimeout;
        private boolean fsync;
        private boolean j;
        public Builder<T> w(T w){
            this.w = w;
            return this;
        }
        public Builder cmdName(int cmdName){
            this.cmdName = cmdName;
            return this;
        }
        public Builder WTimeout(int wtimeout){
            this.WTimeout = wtimeout;
            return this;
        }
        public Builder fsync(boolean fsync){
            this.fsync = fsync;
            return this;
        }
        public Builder j(boolean j){
            this.j = j;
            return this;
        }

        public GetLastError<T> build(){
            return new GetLastError(this.cmdName, this.w, this.WTimeout, this.j);
        }
    }
}
