package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;

import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
public class Server extends BasicDomain {
    String name;
    String ip;
    String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
