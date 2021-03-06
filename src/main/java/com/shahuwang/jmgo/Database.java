package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/12.
 */
public class Database {
    private MongoSession session;
    private String name;

    public Database(MongoSession session, String name){
        this.session = session;
        this.name = name;
        
    }


    public MongoSession getSession() {
        return session;
    }

    public Collection C(String name) {
        return new Collection(this, name, this.name + "." + name);
    }
    public String getName() {
        return name;
    }
}
