package me.rkfg.ns2gather.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import me.rkfg.ns2gather.domain.Player;
import me.rkfg.ns2gather.dto.HiveStatsDTO;
import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.dto.Side;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.LogicException;
import ru.ppsrk.gwt.server.HibernateCallback;
import ru.ppsrk.gwt.server.HibernateUtil;
import ru.ppsrk.gwt.server.LogicExceptionFormatted;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class GatherPlayersManager implements AutoCloseable {

    public enum TeamStatType {
        EQUALITY, NOFREE
    }

    Timer playersCleanupTimer = new Timer("Players cleanup", true);
    ConcurrentHashMap<Long, GatherPlayers> gatherToPlayers = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, PlayerDTO> steamIdPlayer = new ConcurrentHashMap<>();
    ConcurrentHashMap<Long, String> kicked = new ConcurrentHashMap<>();

    public void addKicked(Long steamId, String reason) {
        kicked.put(steamId, reason);
    }

    public String getKickedReason(Long steamId) {
        return kicked.get(steamId);
    }

    public void removeKickedId(Long steamId) {
        kicked.remove(steamId);
    }

    private CleanupCallback cleanupCallback;

    public interface CleanupCallback {
        public void playerRemoved(Long gatherId, PlayerDTO player) throws LogicException, ClientAuthException;
    }

    public class GatherPlayers {
        ConcurrentHashMap<Long, PlayerDTO> gatherPlayers = new ConcurrentHashMap<>();
        ConcurrentHashMap<Long, PlayerDTO> gatherParticipants = new ConcurrentHashMap<>();
        List<Long> comms = new ArrayList<Long>();

        public void putPlayer(PlayerDTO playerDTO) {
            gatherPlayers.put(playerDTO.getId(), playerDTO);
        }

        public PlayerDTO getPlayer(Long steamId) {
            return gatherPlayers.get(steamId);
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
                PlayerDTO comm = getParticipant(commId);
                if (comm == null) {
                    throw LogicExceptionFormatted.format("Не найден командир в списке участников: %d", commId);
                }
                comm.setSide(commSide);
                commSide = commSide == Side.MARINES ? Side.ALIENS : Side.MARINES;
            }
        }

        public void pickPlayer(Long steamId, Long participantSteamId) throws LogicException {
            boolean equalPlayersInTeams = getTeamsStat(TeamStatType.EQUALITY);
            if (equalPlayersInTeams && !steamId.equals(comms.get(1)) || !equalPlayersInTeams && !steamId.equals(comms.get(0))) {
                throw new LogicException("Вы не можете сейчас выбрать игрока.");
            }
            PlayerDTO comm = getParticipant(steamId);
            if (comm == null) {
                throw LogicExceptionFormatted.format("Командир %d не найден среди участников.", steamId);
            }
            Side side = comm.getSide();
            PlayerDTO participant = getParticipant(participantSteamId);
            if (participant == null) {
                throw LogicExceptionFormatted.format("Игрок %d не найден среди участников.", participantSteamId);
            }
            if (participant.getSide() == Side.MERC) {
                throw new LogicException("Вы не можете взять мерка в свою команду.");
            }
            participant.setLastPing(System.currentTimeMillis());
            participant.setSide(side);
        }

        public boolean getTeamsStat(TeamStatType type) {
            int aliens = 0;
            int marines = 0;
            int free = 0;
            for (PlayerDTO playerDTO : gatherParticipants.values()) {
                switch (playerDTO.getSide()) {
                case ALIENS:
                    aliens++;
                    break;
                case MARINES:
                    marines++;
                    break;
                case NONE:
                    free++;
                    break;
                case MERC:
                    // don't count merc(s)
                    break;
                default:
                    break;
                }
            }
            switch (type) {
            case EQUALITY:
                return aliens == marines;
            case NOFREE:
                return free == 0;
            default:
                return false;
            }
        }

        // clone players to fix them for the current gather
        public void playersToParticipants() {
            gatherParticipants = new ConcurrentHashMap<>();
            PlayerDTO merc = null;
            boolean needMerc = false;
            Collection<PlayerDTO> players = getPlayers();
            if (players.size() % 2 == 1) {
                needMerc = true;
            }
            for (PlayerDTO playerDTO : players) {
                PlayerDTO participant = playerDTO.clone();
                if (needMerc) {
                    if (merc == null) {
                        merc = participant;
                    } else {
                        if (merc.getLoginTimestamp() < participant.getLoginTimestamp() && !comms.contains(participant.getId())) {
                            merc = participant;
                        }
                    }

                }
                gatherParticipants.put(playerDTO.getId(), participant);
            }
            if (needMerc && merc != null) {
                merc.setSide(Side.MERC);
            }
        }

        public Collection<PlayerDTO> getParticipants() {
            return gatherParticipants.values();
        }

        public PlayerDTO getParticipant(Long steamId) {
            return gatherParticipants.get(steamId);
        }

        public void remove(PlayerDTO player) {
            gatherPlayers.remove(player.getId());
        }

        public synchronized void getHiveStats(final Long steamId, final AsyncCallback<HiveStatsDTO> callback) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    PlayerDTO playerDTO = getPlayer(steamId);
                    Long realSteamId = steamId & 0xFFFFFFFFL;
                    HttpClient client = NetworkUtils.getHTTPClient();
                    HttpGet request = new HttpGet("http://hive.naturalselection2.com/api/get/playerData/" + realSteamId);
                    try {
                        String resultJSON = NetworkUtils.readStream(client.execute(request).getEntity().getContent());
                        JSONObject rootObject = new JSONObject(resultJSON);
                        HiveStatsDTO hiveStatsDTO = playerDTO.getHiveStats();
                        if (hiveStatsDTO == null) {
                            hiveStatsDTO = new HiveStatsDTO();
                        }
                        hiveStatsDTO.setHoursPlayed(rootObject.getLong("playTime") / 3600);
                        hiveStatsDTO.setSkill(rootObject.getDouble("skill"));
                        playerDTO.setHiveStats(hiveStatsDTO);
                        callback.onSuccess(hiveStatsDTO);
                    } catch (IOException e) {
                        callback.onFailure(e);
                    } catch (JSONException e) {
                        callback.onFailure(e);
                    }
                }
            }, "Hive request for " + steamId).start();
        }

    }

    public GatherPlayersManager() {
        runPlayersCleanup();
    }

    public GatherPlayersManager(CleanupCallback cleanupCallback) {
        this();
        this.cleanupCallback = cleanupCallback;
    }

    public void addPlayerToGather(Long gatherId, PlayerDTO playerDTO) {
        GatherPlayers gatherPlayers = getPlayersByGather(gatherId);
        playerDTO.setLoginTimestamp(System.currentTimeMillis());
        gatherPlayers.putPlayer(playerDTO);
        addPlayerBySteamId(playerDTO);
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

    public void addPlayerBySteamId(PlayerDTO player) {
        steamIdPlayer.put(player.getId(), player);
    }

    public Set<Long> getGathers() {
        return gatherToPlayers.keySet();
    }

    public PlayerDTO lookupPlayerBySteamId(Long steamId) throws LogicException, ClientAuthException {
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
            completeInfo(player);
            addPlayerBySteamId(player);
            return player;
        } catch (IOException | IllegalStateException e) {
            throw new LogicException("can't get player data");
        } catch (JSONException e) {
            throw new LogicException("invalid player data");
        }
    }

    private void completeInfo(final PlayerDTO playerDTO) throws LogicException, ClientAuthException {
        HibernateUtil.exec(new HibernateCallback<Void>() {

            @Override
            public Void run(Session session) throws LogicException, ClientAuthException {
                try {
                    Player playerEnt = (Player) session.createQuery("from Player p where p.steamId = :sid")
                            .setLong("sid", playerDTO.getId()).uniqueResult();
                    if (playerEnt != null) {
                        playerDTO.setNick(playerEnt.getNick());
                    }
                } catch (NonUniqueResultException e) {
                    throw LogicExceptionFormatted.format("Дубликация информации об игроке %d", playerDTO.getId());
                }
                return null;
            }
        });

    }

    private void runPlayersCleanup() {
        playersCleanupTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                for (Long gatherId : getGathers()) {
                    GatherPlayers gatherPlayers = getPlayersByGather(gatherId);
                    for (PlayerDTO player : gatherPlayers.getPlayers()) {
                        if (System.currentTimeMillis() - player.getLastPing() > Settings.PLAYER_PING_TIMEOUT) {
                            gatherPlayers.remove(player);
                            if (cleanupCallback != null) {
                                try {
                                    cleanupCallback.playerRemoved(gatherId, player);
                                } catch (LogicException | ClientAuthException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }, 5000, 5000);
    }

    @Override
    public void close() throws Exception {
        playersCleanupTimer.cancel();
    }
}
