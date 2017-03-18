package com.shahuwang.jmgo;

import org.bson.BsonDocument;
import org.bson.BsonElement;

import java.util.Vector;

/**
 * Created by rickey on 2017/2/28.
 */
public class QueryOp {
    private String collection;
    private BsonDocument query;
    private int skip;
    private int limit;
    private BsonDocument selector;
    private QueryOpFlags flags;
    private IReply replyFuncs;
    private Mode mode;
    private QueryWrapper options;
    private boolean hasOptions;
    private Vector<BsonElement> serverTags;

    private QueryOp(
            String collection, BsonDocument query, int skip, int limit,
            BsonDocument selector, QueryOpFlags flags, IReply replyFuncs,
            Mode mode, QueryWrapper options, boolean hasOptions,
            Vector<BsonElement> serverTags
    ){
        this.collection = collection;
        this.query = query;
        this.skip = skip;
        this.limit = limit;
        this.selector = selector;
        this.flags = flags;
        this.replyFuncs = replyFuncs;
        this.mode = mode;
        this.options = options;
        this.hasOptions = hasOptions;
        this.serverTags = serverTags;
    }

    public BsonDocument getQuery() {
        return query;
    }

    public void setReplyFuncs(IReply replyFuncs) {
        this.replyFuncs = replyFuncs;
    }

    public Vector<BsonElement> getServerTags() {
        return serverTags;
    }

    public static class QueryOpBuilder{
        private String collection;
        private BsonDocument query;
        private int skip;
        private int limit;
        private BsonDocument selector;
        private QueryOpFlags flags;
        private IReply replyFuncs;
        private Mode mode;
        private QueryWrapper options;
        private boolean hasOptions;
        private Vector<BsonElement> serverTags;
        public QueryOpBuilder(String collection, BsonDocument query){
            this.collection = collection;
            this.query = query;
        }

        public QueryOpBuilder skip(int skip){
            this.skip = skip;
            return this;
        }

        public QueryOpBuilder limit(int limit){
            this.limit = limit;
            return this;
        }

        public QueryOpBuilder selector(BsonDocument selector){
            this.selector = selector;
            return this;
        }

        public QueryOpBuilder flags(QueryOpFlags flags){
            this.flags = flags;
            return this;
        }

        public QueryOpBuilder replyFuncs(IReply replyFuncs){
            this.replyFuncs = replyFuncs;
            return this;
        }

        public QueryOpBuilder mode(Mode mode){
            this.mode = mode;
            return this;
        }

        public QueryOpBuilder options(QueryWrapper options){
            this.options = options;
            return this;
        }

        public QueryOpBuilder hasOptions(boolean hasOptions){
            this.hasOptions = hasOptions;
            return  this;
        }

        public  QueryOpBuilder serverTags(Vector<BsonElement> serverTags){
            this.serverTags = this.serverTags;
            return this;
        }

        public QueryOp build(){
            return new QueryOp(
                    collection, query, skip, limit, selector,
                    flags, replyFuncs, mode, options, hasOptions, serverTags
            );
        }
    }
}
