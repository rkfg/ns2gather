package me.rkfg.ns2gather.dto;

import ru.ppsrk.gwt.client.HasId;

public class VoteResultDTO implements HasId {
    Long id;
    Long voteCount;

    public VoteResultDTO(Long id, Long voteCount) {
        super();
        this.id = id;
        this.voteCount = voteCount;
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

}
