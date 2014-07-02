package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;

import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
public class Player extends BasicDomain {
    String nick;
    Long steamId;

    public Player() {
    }

    public Player(String nick, Long steamId) {
        this.nick = nick;
        this.steamId = steamId;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Long getSteamId() {
        return steamId;
    }

    public void setSteamId(Long steamId) {
        this.steamId = steamId;
    }
}
