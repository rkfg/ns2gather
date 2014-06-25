package me.rkfg.ns2gather.dto;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SteamPlayerDTO implements IsSerializable {
    private float connectTime;
    private String name;
    private int score;

    public float getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(float connectTime) {
        this.connectTime = connectTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

}
