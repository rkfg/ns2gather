package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;

import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
public class Server extends BasicDomain {
    String name;
    String ip;
    Integer port;
    Integer webPort;
    String webLogin;
    String webPassword;
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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getWebPort() {
        return webPort;
    }

    public void setWebPort(Integer webPort) {
        this.webPort = webPort;
    }

    public String getWebLogin() {
        return webLogin;
    }

    public void setWebLogin(String webLogin) {
        this.webLogin = webLogin;
    }

    public String getWebPassword() {
        return webPassword;
    }

    public void setWebPassword(String webPassword) {
        this.webPassword = webPassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
