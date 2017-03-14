package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/13.
 */
public class Credential {
    private String username;
    private String password;
    private String source;
    private String service;
    private String serviceHost;
    private String mechanism;

    public Credential(String username, String password, String source, String service, String serviceHost, String mechanism){
        this.username = username;
        this.password = password;
        this.source = source;
        this.service = service;
        this.serviceHost = serviceHost;
        this.mechanism = mechanism;
    }

    public Credential clone() {
        return new Credential(
                this.username, this.password, this.source, this.service, this.serviceHost, this.mechanism
        );
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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
}
