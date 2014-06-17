package me.rkfg.ns2gather.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import me.rkfg.ns2gather.dto.PlayerDTO;
import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.LogicException;

public class GatherPlayersManager {

    HashMap<Long, GatherPlayers> gatherToPlayers = new HashMap<>();
    private CleanupCallback cleanupCallback;

    public interface CleanupCallback {
        public void playerRemoved(Long gatherId, PlayerDTO player) throws LogicException, ClientAuthException;
    }

    public class GatherPlayers extends HashMap<Long, PlayerDTO> {

        /**
         * 
         */
        private static final long serialVersionUID = 6870663843149554308L;

    }

    public GatherPlayersManager() {
        runPlayersCleanup();
    }

    public GatherPlayersManager(CleanupCallback cleanupCallback) {
        this();
        this.cleanupCallback = cleanupCallback;
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

    private void runPlayersCleanup() {
        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                synchronized (this) {
                    for (Long gatherId : getGathers()) {
                        Iterator<Entry<Long, PlayerDTO>> iterator = getPlayersByGather(gatherId).entrySet().iterator();
                        while (iterator.hasNext()) {
                            Entry<Long, PlayerDTO> entry = iterator.next();
                            if (System.currentTimeMillis() - entry.getValue().getLastPing() > Settings.PLAYER_PING_TIMEOUT) {
                                iterator.remove();
                                if (cleanupCallback != null) {
                                    try {
                                        cleanupCallback.playerRemoved(gatherId, entry.getValue());
                                    } catch (LogicException | ClientAuthException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }, 5000, 5000);
    }
}
