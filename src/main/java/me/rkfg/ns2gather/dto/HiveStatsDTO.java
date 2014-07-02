package me.rkfg.ns2gather.dto;

import com.google.gwt.user.client.rpc.IsSerializable;

public class HiveStatsDTO implements IsSerializable {
    Long hoursPlayed;
    Double skill;

    public Long getHoursPlayed() {
        return hoursPlayed;
    }

    public void setHoursPlayed(Long hoursPlayed) {
        this.hoursPlayed = hoursPlayed;
    }

    public Double getSkill() {
        return skill;
    }

    public void setSkill(Double skill) {
        this.skill = skill;
    }

}
