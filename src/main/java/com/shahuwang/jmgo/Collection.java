package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/12.
 */
public class Collection {
    private Database database;
    private String name;
    private String fullName;

    public Collection(Database database, String name, String fullName){
        this.database = database;
        this.name = name;
        this.fullName = fullName;
    }

    public Database getDatabase() {
        return database;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }
}
