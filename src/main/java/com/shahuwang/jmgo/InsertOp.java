package com.shahuwang.jmgo;

import org.bson.BsonDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rickey on 2017/3/15.
 */
public class InsertOp {
    private String collection;
    private int flags;
    private List<BsonDocument> documents;

    Logger logger = LogManager.getLogger(InsertOp.class.getName());
    public InsertOp(String collection, BsonDocument[] documents, int flags){
        this.collection = collection;
        this.flags = flags;
        this.documents = Arrays.asList(documents);
    }

    public String getCollection() {
        return collection;
    }

    public int getFlags() {
        return flags;
    }

    public List<BsonDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<BsonDocument> documents) {
        this.documents = documents;
    }
}
