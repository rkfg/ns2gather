package me.rkfg.ns2gather.dto;

import ru.ppsrk.gwt.client.HasId;

public class VoteResultDTO implements HasId {
    Long id;
    Long voteCount;
    VoteType type;
    CheckedDTO target;

    public VoteResultDTO(Long id, Long voteCount, VoteType type, CheckedDTO target) {
        super();
        this.id = id;
        this.voteCount = voteCount;
        this.type = type;
        this.target = target;
    }

    public VoteResultDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Long voteCount) {
        this.voteCount = voteCount;
    }

    public VoteType getType() {
        return type;
    }

    public void setType(VoteType type) {
        this.type = type;
    }

    public CheckedDTO getTarget() {
        return target;
    }

    public void setTarget(CheckedDTO target) {
        this.target = target;
    }

}
