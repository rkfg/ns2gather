package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;

import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
public class Remembered extends BasicDomain {
    Long rememberId;
    Long steamId;
    Long lastLogin;

    public Remembered(Long rememberId, Long steamId) {
        super();
        this.rememberId = rememberId;
        this.steamId = steamId;
        lastLogin = System.currentTimeMillis();
    }

    public Remembered() {
    }

    public Long getRememberId() {
        return rememberId;
    }

    public void setRememberId(Long rememberId) {
        this.rememberId = rememberId;
    }

    public Long getSteamId() {
        return steamId;
    }

    public void setSteamId(Long steamId) {
        this.steamId = steamId;
    }

    public Long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Long lastLogin) {
        this.lastLogin = lastLogin;
    }

}
