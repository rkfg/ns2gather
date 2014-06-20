package me.rkfg.ns2gather.dto;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;

public class InitStateDTO implements IsSerializable {
    List<MapDTO> maps;
    List<ServerDTO> servers;
    List<PlayerDTO> players;
    String voteStat;
    Set<String> votedNames;
    GatherState gatherState;
    String version;

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

    public String getVoteStat() {
        return voteStat;
    }

    public void setVoteStat(String voteStat) {
        this.voteStat = voteStat;
    }

    public Set<String> getVotedNames() {
        return votedNames;
    }

    public void setVotedNames(Set<String> votedNames) {
        this.votedNames = votedNames;
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

}
