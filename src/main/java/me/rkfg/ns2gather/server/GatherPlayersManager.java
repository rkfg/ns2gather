package me.rkfg.ns2gather.server;

import java.util.HashMap;

import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.server.GatherPlayersManager.GatherPlayers;

public class GatherPlayersManager extends HashMap<Long, GatherPlayers> {

    /**
     * 
     */
    private static final long serialVersionUID = -7988788333824827521L;

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
        GatherPlayers result = get(gatherId);
        if (result == null) {
            result = new GatherPlayers();
            put(gatherId, result);
        }
        return result;
    }

}
