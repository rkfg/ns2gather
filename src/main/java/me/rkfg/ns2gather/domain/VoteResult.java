package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;

import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
public class VoteResult extends BasicDomain {
    Long gatherId;
    VoteType type;
    Long targetId;
    Long voteCount;

    public VoteResult(Long gatherId, VoteType type, Long targetId, Long voteCount) {
        super();
        this.gatherId = gatherId;
        this.type = type;
        this.targetId = targetId;
        this.voteCount = voteCount;
    }

    public VoteResult() {
    }

    public Long getGatherId() {
        return gatherId;
    }

    public void setGatherId(Long gatherId) {
        this.gatherId = gatherId;
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

    public void inc() {
        setVoteCount(getVoteCount() + 1);
    }

}
