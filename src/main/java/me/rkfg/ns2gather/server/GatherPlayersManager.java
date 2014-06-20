package me.rkfg.ns2gather.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import me.rkfg.ns2gather.dto.PlayerDTO;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.LogicException;

public class GatherPlayersManager {

    HashMap<Long, GatherPlayers> gatherToPlayers = new HashMap<>();
    HashMap<Long, PlayerDTO> steamIdName = new HashMap<>();

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
        addPlayerBySteamId(playerDTO.getId(), playerDTO);
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

    public PlayerDTO getNameBySteamId(Long steamId) {
        return steamIdName.get(steamId);
    }

    public void addPlayerBySteamId(Long steamId, PlayerDTO player) {
        steamIdName.put(steamId, player);
    }

    public Set<Long> getGathers() {
        return gatherToPlayers.keySet();
    }

    public PlayerDTO lookupPlayerBySteamId(Long steamId) throws LogicException {
        String name;
        HttpClient client = getHTTPClient();
        HttpUriRequest request = new HttpGet(String.format(
                "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s", Settings.STEAM_API_KEY, steamId));
        try {
            HttpResponse response = client.execute(request);
            JSONObject jsonRootObject = new JSONObject(readStream(response.getEntity().getContent()));
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

    private HttpClient getHTTPClient() {
        RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(Settings.TIMEOUT).setConnectTimeout(Settings.TIMEOUT)
                .setSocketTimeout(Settings.TIMEOUT).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config)
                .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36")
                .build();
    }

    private String readStream(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
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
