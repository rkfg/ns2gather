package me.rkfg.ns2gather.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.dto.Side;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.LogicException;
import ru.ppsrk.gwt.server.LogicExceptionFormatted;

public class GatherPlayersManager {

    HashMap<Long, GatherPlayers> gatherToPlayers = new HashMap<>();
    HashMap<Long, PlayerDTO> steamIdPlayer = new HashMap<>();

    private CleanupCallback cleanupCallback;

    public interface CleanupCallback {
        public void playerRemoved(Long gatherId, PlayerDTO player) throws LogicException, ClientAuthException;
    }

    public class GatherPlayers {
        HashMap<Long, PlayerDTO> gatherPlayers = new HashMap<>();
        List<Long> comms = new ArrayList<Long>();

        public void putPlayer(Long id, PlayerDTO playerDTO) {
            gatherPlayers.put(id, playerDTO);
        }

        public PlayerDTO getPlayer(Long steamId) {
            return gatherPlayers.get(steamId);
        }

        public Set<Entry<Long, PlayerDTO>> playersEntrySet() {
            return gatherPlayers.entrySet();
        }

        public Collection<PlayerDTO> getPlayers() {
            return gatherPlayers.values();
        }

        public int playerCount() {
            return gatherPlayers.size();
        }

        public Long getCommId(int i) {
            return comms.get(i);
        }

        public void setComms(List<Long> comms) {
            this.comms = comms;
        }

        public void pickSide(final Long steamId, final Side side) throws LogicException, ClientAuthException {
            if (!getCommId(0).equals(steamId)) {
                throw new LogicException("Вам нельзя голосовать за сторону.");
            }
            Side commSide = side;
            for (int i = 0; i < 2; i++) {
                Long commId = getCommId(i);
                PlayerDTO comm = getPlayer(commId);
                if (comm == null) {
                    throw LogicExceptionFormatted.format("Не найден командир в списке участников: %d", commId);
                }
                comm.setSide(commSide);
                commSide = commSide == Side.MARINES ? Side.ALIENS : Side.MARINES;
            }
        }
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
        gatherPlayers.putPlayer(playerDTO.getId(), playerDTO);
        addPlayerBySteamId(playerDTO.getId(), playerDTO);
    }

    public PlayerDTO getPlayerByGatherSteamId(Long gatherId, Long steamId) {
        return getPlayersByGather(gatherId).getPlayer(steamId);
    }

    public GatherPlayers getPlayersByGather(Long gatherId) {
        GatherPlayers result = gatherToPlayers.get(gatherId);
        if (result == null) {
            result = new GatherPlayers();
            gatherToPlayers.put(gatherId, result);
        }
        return result;
    }

    public PlayerDTO getPlayerBySteamId(Long steamId) {
        return steamIdPlayer.get(steamId);
    }

    public void addPlayerBySteamId(Long steamId, PlayerDTO player) {
        steamIdPlayer.put(steamId, player);
    }

    public Set<Long> getGathers() {
        return gatherToPlayers.keySet();
    }

    public PlayerDTO lookupPlayerBySteamId(Long steamId) throws LogicException {
        String name;
        HttpClient client = NetworkUtils.getHTTPClient();
        HttpUriRequest request = new HttpGet(String.format(
                "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s", Settings.STEAM_API_KEY, steamId));
        try {
            HttpResponse response = client.execute(request);
            JSONObject jsonRootObject = new JSONObject(NetworkUtils.readStream(response.getEntity().getContent()));
            JSONObject jsonResponse = jsonRootObject.optJSONObject("response");
            if (jsonResponse == null) {
                throw new LogicException("no response");
            }
            JSONArray jsonPlayers = jsonResponse.optJSONArray("players");
            if (jsonPlayers == null || jsonPlayers.length() == 0) {
                throw new LogicException("no players");
            }
            JSONObject jsonPlayer = jsonPlayers.optJSONObject(0);
            if (jsonPlayer == null) {
                throw new LogicException("no player");
            }
            name = jsonPlayer.optString("personaname");
            if (name == null) {
                throw new LogicException("no persona name");
            }
            String profileUrl = jsonPlayer.optString("profileurl");
            if (profileUrl == null) {
                throw new LogicException("no profile url");
            }
            PlayerDTO player = new PlayerDTO(steamId, name, profileUrl, System.currentTimeMillis());
            addPlayerBySteamId(steamId, player);
            return player;
        } catch (IOException | IllegalStateException e) {
            throw new LogicException("can't get player data");
        } catch (JSONException e) {
            throw new LogicException("invalid player data");
        }
    }

    private void runPlayersCleanup() {
        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                synchronized (this) {
                    for (Long gatherId : getGathers()) {
                        Iterator<Entry<Long, PlayerDTO>> iterator = getPlayersByGather(gatherId).playersEntrySet().iterator();
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
