package me.rkfg.ns2gather.dto;

public class RuleDTO {
    int votesRequired;
    int votesLimit;
    int winnerCount;
    String name;

    public RuleDTO(int votesRequired, int votesLimit, int winnerCount, String name) {
        super();
        this.votesRequired = votesRequired;
        this.votesLimit = votesLimit;
        this.winnerCount = winnerCount;
        this.name = name;
    }

    public int getVotesRequired() {
        return votesRequired;
    }

    public void setVotesRequired(int votesRequired) {
        this.votesRequired = votesRequired;
    }

    public int getVotesLimit() {
        return votesLimit;
    }

    public void setVotesLimit(int votesLimit) {
        this.votesLimit = votesLimit;
    }

    public int getWinnerCount() {
        return winnerCount;
    }

    public void setWinnerCount(int winnerCount) {
        this.winnerCount = winnerCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String voteRange() {
        if (votesRequired != votesLimit) {
            return votesRequired + "-" + votesLimit;
        } else {
            return String.valueOf(votesRequired);
        }
    }
}
