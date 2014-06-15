package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
@FilterDef(name = "pv_gatherId", parameters = @ParamDef(name = "gid", type = "long"))
@Filter(name = "pv_gatherId", condition = "gatherId = :gid")
public class Vote extends BasicDomain {
    @ManyToOne
    PlayerVote player;
    Long targetId;
    VoteType type;
    Long gatherId;

    public Vote(PlayerVote player, Long targetId, VoteType type, Long gatherId) {
        this.player = player;
        this.targetId = targetId;
        this.type = type;
        this.gatherId = gatherId;
    }

    public Vote() {
    }

    public PlayerVote getPlayer() {
        return player;
    }

    public void setPlayer(PlayerVote player) {
        this.player = player;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public VoteType getType() {
        return type;
    }

    public void setType(VoteType type) {
        this.type = type;
    }

    public Long getGatherId() {
        return gatherId;
    }

    public void setGatherId(Long gatherId) {
        this.gatherId = gatherId;
    }

}
