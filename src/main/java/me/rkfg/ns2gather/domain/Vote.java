package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
public class Vote extends BasicDomain {
    @ManyToOne
    PlayerVote player;
    Long targetId;
    VoteType type;
    @ManyToOne
    Gather gather;

    public Vote(PlayerVote player, Long targetId, VoteType type, Gather gather) {
        this.player = player;
        this.targetId = targetId;
        this.type = type;
        this.gather = gather;
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

    public Gather getGather() {
        return gather;
    }

    public void setGather(Gather gather) {
        this.gather = gather;
    }

}
