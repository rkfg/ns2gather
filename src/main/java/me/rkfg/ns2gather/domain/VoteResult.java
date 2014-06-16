package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
public class VoteResult extends BasicDomain {
    @ManyToOne
    Gather gather;
    VoteType type;
    Long targetId;
    Long voteCount;
    Long place;

    public VoteResult(Gather gather, VoteType type, Long targetId, Long voteCount) {
        super();
        this.gather = gather;
        this.type = type;
        this.targetId = targetId;
        this.voteCount = voteCount;
    }

    public VoteResult() {
    }

    public Gather getGather() {
        return gather;
    }

    public void setGather(Gather gather) {
        this.gather = gather;
    }

    public VoteType getType() {
        return type;
    }

    public void setType(VoteType type) {
        this.type = type;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Long voteCount) {
        this.voteCount = voteCount;
    }

    public Long getPlace() {
        return place;
    }

    public void setPlace(Long place) {
        this.place = place;
    }

    public void inc() {
        setVoteCount(getVoteCount() + 1);
    }

}
