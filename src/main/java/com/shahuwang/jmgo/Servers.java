package com.shahuwang.jmgo;

import java.util.Comparator;
import java.util.Vector;

/**
 * Created by rickey on 2017/3/1.
 */
public class Servers {
    private Vector<MongoServer> slice;

    public MongoServer search(ServerAddr addr){
        for(MongoServer server: this.slice){
            if (server.getAddr().getTcpaddr().equals(addr.getTcpaddr())){
                return server;
            }
        }
        return null;
    }

    public void add(MongoServer server){
        this.slice.add(server);
        this.slice.sort(new ServerComparator());
    }

    public MongoServer remove(MongoServer other){
        MongoServer server = this.search(other.getAddr());
        if (server != null){
            this.slice.remove(server);
        }
        return server;
    }

    class ServerComparator implements Comparator<MongoServer>{
        public int compare(MongoServer s1, MongoServer s2){
            String host1 = s1.getAddr().getTcpaddr().getAddress().getCanonicalHostName();
            String host2 = s2.getAddr().getTcpaddr().getAddress().getCanonicalHostName();
            return host1.compareTo(host2);
        }
    }
}
