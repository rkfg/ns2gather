package me.rkfg.ns2gather.dto;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;

public class InitStateDTO implements IsSerializable {
    List<MapDTO> maps;
    List<ServerDTO> servers;
    List<PlayerDTO> players;
    List<VoteResultDTO> voteResults;
    String voteStat;
    Set<Long> votedIds;
    GatherState gatherState;
    String version;
    String passwords;

    public List<MapDTO> getMaps() {
        return maps;
    }

    public void setMaps(List<MapDTO> maps) {
        this.maps = maps;
    }

    public List<ServerDTO> getServers() {
        return servers;
    }

    public void setServers(List<ServerDTO> servers) {
        this.servers = servers;
    }

    public List<PlayerDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }

    public List<VoteResultDTO> getVoteResults() {
        return voteResults;
    }

    public void setVoteResults(List<VoteResultDTO> voteResults) {
        this.voteResults = voteResults;
    }

    public String getVoteStat() {
        return voteStat;
    }

    public void setVoteStat(String voteStat) {
        this.voteStat = voteStat;
    }

    public Set<Long> getVotedIds() {
        return votedIds;
    }

    public void setVotedIds(Set<Long> votedIds) {
        this.votedIds = votedIds;
    }

    public GatherState getGatherState() {
        return gatherState;
    }

    public void setGatherState(GatherState gatherState) {
        this.gatherState = gatherState;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPasswords() {
        return passwords;
    }

    public void setPasswords(String passwords) {
        this.passwords = passwords;
    }

}
