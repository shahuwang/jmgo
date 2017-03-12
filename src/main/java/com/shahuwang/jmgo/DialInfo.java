package com.shahuwang.jmgo;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.shahuwang.jmgo.exceptions.JmgoException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by rickey on 2017/3/12.
 */
public class DialInfo {
    private String[] addrs; // 提供的初始地址列表
    private boolean direct; // 直接和提供的地址联系还是和整个cluster联系
    private Duration timeout; // 0 永远不超时
    private boolean failFast; // 遇到一个mongo服务部可用，直接报错，不等系统自动去尝试多次
    private String database;
    private String replicaSetName; // 如果指定，只和该replica联系
    private String source; // 用于认证和权限的数据库名称
    private String service; // 确定认证时使用GSSAPI是对应的服务， 默认是"mongodb"
    private String serviceHost; // GSSAPI 协议使用的host
    private String mechanism; // 确定认证时使用的协议 https://docs.mongodb.com/manual/core/authentication-mechanisms/
    private String userName;
    private String password;
    private int poolLimit; // 每个服务器的socket池数量大小

    Logger logger = LogManager.getLogger(Cluster.class.getName());

    public DialInfo(String url)throws JmgoException {
        parseURL(url);
    }

    private void parseURL(String url) throws JmgoException {
        UrlInfo uinfo = extractURL(url);
        boolean direct = false;
        String mechanism = "";
        String service = "";
        String source = "";
        String setName = "";
        int poolLimit = 0;
        for(String k: uinfo.options.keySet()){
            String v = uinfo.options.get(k);
            if(k == "authSource") {
                source = v;
            }
            if(k == "authMechanism"){
                mechanism = v;
            }
            if(k == "gassapiServiceName"){
                service = v;
            }
            if(k == "replicaSet") {
                setName = v;
            }
            if(k == "maxPoolSize") {
                try {
                    poolLimit = Integer.parseInt(v);
                }catch (NumberFormatException e){
                    logger.catching(e);
                    throw new JmgoException("bad value for maxPoolSize: " + v);
                }
            }
            if(k == "connect"){
                if (v == "direct"){
                    direct = true;

                }
                if( v == "replicaSet"){

                }
            }
            throw new JmgoException("unsupported connection URL options: " + k + "=" + v);
        }
        this.addrs = uinfo.addrs;
        this.direct = direct;
        this.database = uinfo.db;
        this.userName = uinfo.user;
        this.password = uinfo.pass;
        this.mechanism = mechanism;
        this.service = service;
        this.source = source;
        this.poolLimit = poolLimit;
        this.replicaSetName = setName;
    }

    public String[] getAddrs() {
        return addrs;
    }

    public boolean isDirect() {
        return direct;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public String getDatabase() {
        return database;
    }

    public String getReplicaSetName() {
        return replicaSetName;
    }

    public String getSource() {
        return source;
    }

    public String getService() {
        return service;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    public String getMechanism() {
        return mechanism;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public int getPoolLimit() {
        return poolLimit;
    }

    private class UrlInfo{
        public String[] addrs;
        public String user;
        public String pass;
        public String db;
        public Map<String, String> options = new HashMap<>();
    }

    private UrlInfo extractURL(String s) throws JmgoException{
        if(s.startsWith("mongodb://")){
            s = s.substring(10);
        }
        UrlInfo info = new UrlInfo();
        int c = s.indexOf("?");
        if (c != -1) {
            // 解析options部分
            String optionPart = s.substring(c+1);
            StringTokenizer token = new StringTokenizer(optionPart, "&", false);
            while (token.hasMoreTokens()) {
                String next = token.nextToken();
                if(next.endsWith(";")) {
                    next = next.replaceAll(";", "");
                }
                String[] slice = next.split("=", 2);
                if (slice.length != 2 || slice[0] == "" || slice[1] == "") {
                    logger.info("connection option must be key=value: {}", next);
                    throw new JmgoException("connection option must be key=value");
                }
                info.options.put(slice[0], slice[1]);
            }
            s = s.substring(0, c);
        }
        c = s.indexOf("@");
        if(c != -1) {
            // 解析用户名和密码部分
            String cred = s.substring(0, c);
            String[] pair = cred.split(":", 2);
            if (pair.length > 2 || pair[0] == "") {
                throw new JmgoException("credentials must be provided as user:pass@host");
            }
            try {
                info.user = URLDecoder.decode(pair[0], "UTF-8");
            }catch (UnsupportedEncodingException e){
                logger.catching(e);
                throw new JmgoException("Cannot unescape username in URL " + pair[0]);
            }
            if (pair.length > 1) {
                try {
                    info.pass = URLDecoder.decode(pair[1], "UTF-8");
                }catch (UnsupportedEncodingException e){
                    logger.catching(e);
                    throw new JmgoException("Cannot unescape password in URL " + pair[1]);
                }
            }

            s = s.substring(c+1);
        }
        c = s.indexOf("/");
        if (c != -1) {
            info.db = s.substring(c+1);
            s = s.substring(0, c);
        }
        info.addrs = s.split(",");
        return info;
    }
}
