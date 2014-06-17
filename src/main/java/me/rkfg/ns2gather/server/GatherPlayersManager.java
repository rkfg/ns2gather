package me.rkfg.ns2gather.server;

import java.util.HashMap;
import java.util.Set;

import me.rkfg.ns2gather.dto.PlayerDTO;

public class GatherPlayersManager {

    HashMap<Long, GatherPlayers> gatherToPlayers = new HashMap<>();

    public class GatherPlayers extends HashMap<Long, PlayerDTO> {

        /**
         * 
         */
        private static final long serialVersionUID = 6870663843149554308L;

    }

    public void addPlayer(Long gatherId, PlayerDTO playerDTO) {
        GatherPlayers gatherPlayers = getPlayersByGather(gatherId);
        gatherPlayers.put(playerDTO.getId(), playerDTO);
    }

    public PlayerDTO getPlayerByGatherSteamId(Long gatherId, Long steamId) {
        return getPlayersByGather(gatherId).get(steamId);
    }

    public GatherPlayers getPlayersByGather(Long gatherId) {
        GatherPlayers result = gatherToPlayers.get(gatherId);
        if (result == null) {
            result = new GatherPlayers();
            gatherToPlayers.put(gatherId, result);
        }
        return result;
    }

    public Set<Long> getGathers() {
        return gatherToPlayers.keySet();
    }

}
