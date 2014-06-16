package me.rkfg.ns2gather.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import me.rkfg.ns2gather.client.NS2G;
import me.rkfg.ns2gather.client.NS2GService;
import me.rkfg.ns2gather.domain.Gather;
import me.rkfg.ns2gather.domain.PlayerVote;
import me.rkfg.ns2gather.domain.Vote;
import me.rkfg.ns2gather.domain.VoteResult;
import me.rkfg.ns2gather.domain.VoteType;
import me.rkfg.ns2gather.dto.GatherState;
import me.rkfg.ns2gather.dto.MapDTO;
import me.rkfg.ns2gather.dto.MessageDTO;
import me.rkfg.ns2gather.dto.MessageType;
import me.rkfg.ns2gather.dto.MessageVisibility;
import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.VoteResultDTO;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.MessageException;

import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.LogicException;
import ru.ppsrk.gwt.server.HibernateCallback;
import ru.ppsrk.gwt.server.HibernateUtil;
import ru.ppsrk.gwt.server.LogicExceptionFormatted;
import ru.ppsrk.gwt.server.LongPollingServer;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class NS2GServiceImpl extends RemoteServiceServlet implements NS2GService {

    private enum ResetType {
        ALL, VOTES, RESULTS
    }

    private static final boolean forceDebug = false;
    private static final boolean forceRelease = false;

    static ConsumerManager manager = new ConsumerManager();
    GatherPlayersManager connectedPlayers = new GatherPlayersManager();
    HashMap<Long, String> steamIdName = new HashMap<>();

    Long playerId = 1L;
    ConcurrentLinkedQueue<MessageDTO> messages = new ConcurrentLinkedQueue<>();
    Object voteCountLock = new Object();
    Object findGatherLock = new Object();
    private boolean debug = false;

    private class MessagePollingServer extends LongPollingServer<List<MessageDTO>> {

        private long since;

        public MessagePollingServer(long period, long execDelay, long since) {
            super(period, execDelay);
            this.since = since;
        }

        @Override
        public List<MessageDTO> exec() throws LogicException, ClientAuthException, ClientAuthException {
            List<MessageDTO> result = new LinkedList<>();
            for (MessageDTO messageDTO : messages) {
                if (messageDTO.getTimestamp() > since) {
                    if (messageDTO.getVisibility() == MessageVisibility.BROADCAST && messageDTO.getGatherId() == getCurrentGatherId()
                            || messageDTO.getVisibility() == MessageVisibility.PERSONAL && messageDTO.getTo().equals(getSteamId())) {
                        result.add(messageDTO);
                    }
                }
                if (System.currentTimeMillis() - messageDTO.getTimestamp() > Settings.MESSAGE_CLEANUP_INTERVAL) {
                    messages.remove(messageDTO);
                }
            }
            if (result.isEmpty()) {
                return null;
            }
            return result;
        }
    }

    public NS2GServiceImpl() {
        debug = HibernateUtil.initSessionFactoryDebugRelease(forceDebug, forceRelease, "hibernate_dev.cfg.xml", "hibernate.cfg.xml");
        runPlayersCleanup();
        runMessageCleanup();
        try {
            resetVotes(null, ResetType.ALL);
        } catch (LogicException | ClientAuthException e) {
            e.printStackTrace();
        }
    }

    private void resetVotes(final Long gatherId, final ResetType type) throws LogicException, ClientAuthException {
        HibernateUtil.exec(new HibernateCallback<Void>() {

            @Override
            public Void run(Session session) throws LogicException, ClientAuthException {
                if (gatherId != null) {
                    session.enableFilter("gatherId").setParameter("gid", gatherId);
                }
                if (type == ResetType.ALL || type == ResetType.VOTES) {
                    @SuppressWarnings("unchecked")
                    List<PlayerVote> playerVotes = session.createQuery(
                            "select pv from PlayerVote pv left join pv.votes v left join v.gather").list();
                    for (PlayerVote playerVote : playerVotes) {
                        session.delete(playerVote);
                    }
                }
                if (type == ResetType.ALL || type == ResetType.RESULTS) {
                    @SuppressWarnings("unchecked")
                    List<VoteResult> voteResults = session.createQuery("select vr from VoteResult vr left join vr.gather").list();
                    for (VoteResult voteResult : voteResults) {
                        session.delete(voteResult);
                    }
                }
                return null;
            }
        });
    }

    private void runPlayersCleanup() {
        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                synchronized (connectedPlayers) {
                    for (Long gatherId : connectedPlayers.keySet()) {
                        Iterator<Entry<Long, PlayerDTO>> iterator = connectedPlayers.get(gatherId).entrySet().iterator();
                        while (iterator.hasNext()) {
                            Entry<Long, PlayerDTO> entry = iterator.next();
                            if (System.currentTimeMillis() - entry.getValue().getLastPing() > Settings.PLAYER_PING_TIMEOUT) {
                                iterator.remove();
                                try {
                                    removeVotes(entry.getValue().getId());
                                    postMessage(MessageType.USER_LEAVES, entry.getValue().getName(), gatherId);
                                    postVoteChangeMessage(gatherId);
                                    updateGatherStateByPlayerNumber(getGatherById(gatherId));
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

    private void runMessageCleanup() {
        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                for (MessageDTO messageDTO : messages) {
                    if (System.currentTimeMillis() - messageDTO.getTimestamp() > Settings.MESSAGE_CLEANUP_INTERVAL) {
                        messages.remove(messageDTO);
                    }
                }
            }
        }, 10000, 10000);
    }

    @Override
    public String login() throws LogicException {
        try {
            // perform discovery on the user-supplied identifier
            List<?> discoveries = manager.discover("http://steamcommunity.com/openid");

            // attempt to associate with the OpenID provider
            // and retrieve one service endpoint for authentication
            DiscoveryInformation discovered = manager.associate(discoveries);

            // store the discovery information in the user's session for later use
            // leave out for stateless operation / if there is no session
            getSession().setAttribute("discovered", discovered);

            // obtain a AuthRequest message to be sent to the OpenID provider
            AuthRequest authReq = manager.authenticate(discovered, Settings.CALLBACK_URL);
            String result = authReq.getDestinationUrl(true);
            return result;
        } catch (MessageException | ConsumerException | DiscoveryException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Long getSteamId() throws ClientAuthException, LogicException {
        Long result = (Long) getSession().getAttribute(Settings.STEAMID_SESSION);
        if (result == null) {
            result = rememberMe();
            if (result == null) {
                throw new ClientAuthException("not logged in");
            }
        }
        return result;
    }

    private Long rememberMe() throws LogicException, ClientAuthException {
        return HibernateUtil.exec(new HibernateCallback<Long>() {

            @Override
            public Long run(Session session) throws LogicException, ClientAuthException {
                Long rid = null;
                for (Cookie cookie : perThreadRequest.get().getCookies()) {
                    if (cookie.getName().equals("rememberSteamId")) {
                        try {
                            rid = Long.valueOf(cookie.getValue());
                            break;
                        } catch (NumberFormatException e) {
                            throw new LogicException("Invalid remember cookie value.");
                        }
                    }
                }
                Long steamId = null;
                if (rid != null) {
                    try {
                        steamId = (Long) session.createQuery("select r.steamId from Remembered r where r.rememberId = :rid")
                                .setLong("rid", rid).uniqueResult();
                        getSession().setAttribute(Settings.STEAMID_SESSION, steamId);
                    } catch (NonUniqueResultException e) {
                        throw new LogicException("Дублирующийся id в БД, автовход отклонён.");
                    }
                }
                return steamId;
            }
        });
    }

    @Override
    public String getUserName() throws LogicException, ClientAuthException {
        return getUserName(null);
    }

    public String getUserName(Long steamId) throws LogicException, ClientAuthException {
        if (steamId == null) {
            steamId = getSteamId();
        }
        String name = steamIdName.get(steamId);
        if (name != null) {
            return name;
        }
        if (debug) {
            name = "fake" + steamId.toString();
            steamIdName.put(steamId, name);
            ping();
            return name;
        }
        HttpClient client = getHTTPClient();
        HttpUriRequest request = new HttpGet(
                String.format("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s",
                        Settings.STEAM_API_KEY, getSteamId()));
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
            steamIdName.put(steamId, name);
            ping();
            return name;
        } catch (IOException | IllegalStateException e) {
            throw new LogicException("can't get player data");
        } catch (JSONException e) {
            throw new LogicException("invalid player data");
        } finally {
        }
    }

    @Override
    public List<ServerDTO> getServers() throws LogicException, ClientAuthException {
        return HibernateUtil.queryList("from Server", new String[] {}, new Object[] {}, ServerDTO.class);
    }

    @Override
    public List<MapDTO> getMaps() throws LogicException, ClientAuthException {
        return HibernateUtil.queryList("from Map", new String[] {}, new Object[] {}, MapDTO.class);
    }

    public static HttpClient getHTTPClient() {
        RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(Settings.TIMEOUT).setConnectTimeout(Settings.TIMEOUT)
                .setSocketTimeout(Settings.TIMEOUT).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config)
                .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36")
                .build();
    }

    public static String readStream(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    @Override
    public void ping() throws ClientAuthException, LogicException {
        synchronized (connectedPlayers) {
            Long steamId = getSteamId();
            Long gatherId = getCurrentGatherId();
            PlayerDTO existing = connectedPlayers.getPlayerByGatherSteamId(gatherId, steamId);
            if (existing == null) {
                String name = steamIdName.get(steamId);
                if (name == null) {
                    return;
                }
                existing = new PlayerDTO(steamId, name, System.currentTimeMillis());
                connectedPlayers.addPlayer(gatherId, existing);
                postMessage(MessageType.USER_ENTERS, name);
                postVoteChangeMessage();
                updateGatherStateByPlayerNumber();
            } else {
                existing.setLastPing(System.currentTimeMillis());
            }
        }
    }

    private void updateGatherStateByPlayerNumber() throws LogicException, ClientAuthException {
        updateGatherStateByPlayerNumber(getCurrentGather());
    }

    private void updateGatherStateByPlayerNumber(Gather gather) throws LogicException, ClientAuthException {
        if (gather.getState() == GatherState.COMPLETED) {
            // nothing to do here
            return;
        }
        Long gatherId = gather.getId();
        if (connectedPlayers.getPlayersByGather(gatherId).size() >= 3) {
            if (gather.getState() == GatherState.OPEN) {
                // close gather if 12 or more players here
                updateGatherState(gather, GatherState.CLOSED);
            }
        } else {
            if (gather.getState() == GatherState.CLOSED) {
                // reopen gather if players have left
                updateGatherState(gather, GatherState.OPEN);
            }
        }
    }

    private void updateGatherState(Gather gather, GatherState newState) throws LogicException, ClientAuthException {
        gather.setState(newState);
        postMessage(MessageType.GATHER_STATUS, String.valueOf(newState.ordinal()), gather.getId());
        HibernateUtil.saveObject(gather);
    }

    private void postMessage(MessageDTO messageDTO) {
        messages.offer(messageDTO);
    }

    @Override
    public List<MessageDTO> getNewMessages(Long since) {
        try {
            return new MessagePollingServer(30000, 100, since).start();
        } catch (InterruptedException | LogicException | ClientAuthException e) {
            return null;
        }
    }

    @Override
    public void sendChatMessage(String text) throws ClientAuthException, LogicException {
        postMessage(MessageType.CHAT_MESSAGE, getUserName() + ": " + text);
    }

    @Override
    public List<PlayerDTO> getConnectedPlayers() throws LogicException, ClientAuthException {
        List<PlayerDTO> result = new LinkedList<>();
        result.addAll(connectedPlayers.getPlayersByGather(getCurrentGatherId()).values());
        Collections.sort(result, new Comparator<PlayerDTO>() {

            @Override
            public int compare(PlayerDTO o1, PlayerDTO o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return result;
    }

    @Override
    public void vote(final Long[][] votes) throws LogicException, ClientAuthException {
        if (getCurrentGather().getState() == GatherState.COMPLETED) {
            throw new LogicException("Голосование уже завершено.");
        }
        if (votes.length != 3) {
            throw LogicExceptionFormatted.format("invalid vote size, expected %d, got %d", 3, votes.length);
        }
        validateVoteNumbers(votes);
        Long gatherId = getCurrentGatherId();
        HibernateUtil.exec(new HibernateCallback<Void>() {

            @Override
            public Void run(Session session) throws LogicException, ClientAuthException {
                Long steamId = getSteamId();
                // delete all previous votes
                removeVotes(steamId);
                PlayerVote playerVote = new PlayerVote(steamId);
                for (int i = 0; i < 3; i++) {
                    Long[] votesCat = votes[i];
                    for (Long targetId : votesCat) {
                        Vote vote = new Vote(playerVote, targetId, VoteType.values()[i], getCurrentGather());
                        playerVote.getVotes().add(vote);
                    }
                }
                session.merge(playerVote);
                return null;
            }
        });
        postVoteChangeMessage();
        postMessage(MessageType.USER_READY, getUserName());
        if (getVotedPlayersCount(gatherId) >= connectedPlayers.getPlayersByGather(gatherId).size()) {
            countResults(gatherId);
        }
    }

    protected Gather getCurrentGather() throws LogicException, ClientAuthException {
        return getGatherById(getCurrentGatherId());
    }

    protected Gather getGatherById(Long gatherId) throws LogicException, ClientAuthException {
        return HibernateUtil.tryGetObject(gatherId, Gather.class, "Не удалось найти gather id=" + gatherId
                + " в БД. Операция не выполнена.");
    }

    private void countResults(final Long gatherId) throws LogicException, ClientAuthException {
        synchronized (voteCountLock) {
            final Gather gather = getGatherById(gatherId);
            try {
                HibernateUtil.exec(new HibernateCallback<Void>() {

                    @Override
                    public Void run(Session session) throws LogicException, ClientAuthException {
                        session.enableFilter("gatherId").setParameter("gid", gatherId);
                        resetVotes(gatherId, ResetType.RESULTS);
                        for (VoteType voteType : VoteType.values()) {
                            @SuppressWarnings("unchecked")
                            List<Vote> votesForType = session.createQuery("select v from Vote v left join v.gather where v.type = :vt")
                                    .setParameter("vt", voteType).list();
                            for (Vote vote : votesForType) {
                                Long targetId = vote.getTargetId();
                                try {
                                    VoteResult existingVoteResult = (VoteResult) session
                                            .createQuery(
                                                    "select vr from VoteResult vr left join vr.gather where vr.type = :vt and vr.targetId = :tid")
                                            .setParameter("vt", voteType).setLong("tid", targetId).uniqueResult();
                                    if (existingVoteResult == null) {
                                        existingVoteResult = (VoteResult) session.merge(new VoteResult(gather, voteType, targetId, 0L));
                                    }
                                    existingVoteResult.inc();
                                } catch (NonUniqueResultException e) {
                                    throw LogicExceptionFormatted.format("Обнаружен дубликат результата голосования: %d, %d, %d",
                                            voteType.ordinal(), gatherId, vote.getTargetId());
                                }
                            }
                            List<VoteResult> results = getVoteResultsByType(gatherId, session, voteType);
                            long i = 0;
                            for (VoteResult voteResult : results) {
                                voteResult.setPlace(i++);
                            }
                            @SuppressWarnings("unchecked")
                            List<VoteResult> toCrop = session
                                    .createQuery(
                                            "select vr from VoteResult vr left join vr.gather where vr.type = :vt and vr not in (:vrl)")
                                    .setParameter("vt", voteType).setParameterList("vrl", results).list();
                            for (VoteResult voteResult : toCrop) {
                                session.delete(voteResult);
                            }
                        }
                        return null;
                    }
                });
            } catch (LogicException e) {
                resetVotes(gatherId, ResetType.ALL);
                postMessage(MessageType.VOTE_ENDED, e.getMessage());
                postVoteChangeMessage();
                return;
            }
            postMessage(MessageType.VOTE_ENDED, "ok");
            updateGatherState(gather, GatherState.COMPLETED);
        }
    }

    @Override
    public List<VoteResultDTO> getVoteResults() throws LogicException, ClientAuthException {
        return HibernateUtil.exec(new HibernateCallback<List<VoteResultDTO>>() {

            @Override
            public List<VoteResultDTO> run(Session session) throws LogicException, ClientAuthException {
                Long gatherId = getCurrentGatherId();
                List<VoteResultDTO> result = new LinkedList<>();
                for (VoteType voteType : VoteType.values()) {
                    List<VoteResult> voteResults = getVoteResultsByType(gatherId, session, voteType);
                    for (VoteResult voteResult : voteResults) {
                        result.add(new VoteResultDTO(voteResult.getTargetId(), voteResult.getVoteCount()));
                    }
                }
                return result;
            }
        });
    }

    protected Long getCurrentGatherId() throws LogicException, ClientAuthException {
        Long gatherId = (Long) getSession().getAttribute(Settings.GATHER_ID);
        if (gatherId == null) {
            gatherId = findOpenGatherId();
            getSession().setAttribute(Settings.GATHER_ID, gatherId);
        }
        return gatherId;
    }

    private HttpSession getSession() throws LogicException {
        if (perThreadRequest == null || perThreadRequest.get() == null) {
            throw new LogicException("session lost");
        }
        return perThreadRequest.get().getSession();
    }

    private Long findOpenGatherId() throws LogicException, ClientAuthException {
        synchronized (findGatherLock) {
            return HibernateUtil.exec(new HibernateCallback<Long>() {

                @Override
                public Long run(Session session) throws LogicException, ClientAuthException {
                    Gather gather = (Gather) session.createQuery("from Gather g where g.state = :open")
                            .setParameter("open", GatherState.OPEN).setMaxResults(1).uniqueResult();
                    if (gather == null) {
                        // create new gather
                        gather = (Gather) session.merge(new Gather());
                    }
                    return gather.getId();
                }
            });
        }
    }

    private void postVoteChangeMessage() throws LogicException, ClientAuthException {
        postVoteChangeMessage(getCurrentGatherId());
    }

    private void postVoteChangeMessage(Long gatherId) throws LogicException, ClientAuthException {
        postMessage(MessageType.VOTE_CHANGE,
                String.format("%d/%d", getVotedPlayersCount(gatherId), connectedPlayers.getPlayersByGather(gatherId).size()), gatherId);
    }

    private Long getVotedPlayersCount(final Long gatherId) throws LogicException, ClientAuthException {
        return HibernateUtil.exec(new HibernateCallback<Long>() {

            @Override
            public Long run(Session session) throws LogicException, ClientAuthException {
                return (Long) session
                        .createQuery(
                                "select count(*) from PlayerVote pv where pv in (select v.player from Vote v where v.gather.id = :gid)")
                        .setLong("gid", gatherId).uniqueResult();
            }
        });
    }

    private void postMessage(MessageType type, String content) throws LogicException, ClientAuthException {
        postMessage(type, content, getCurrentGatherId());
    }

    private void postMessage(MessageType type, String content, Long gatherId) throws LogicException, ClientAuthException {
        postMessage(new MessageDTO(type, content, gatherId));
    }

    private void validateVoteNumbers(Long[][] votes) throws LogicException, ClientAuthException {
        int idx = 0;
        for (Long[] vote : votes) {
            if (vote.length < NS2G.voteRules[idx].getVotesRequired() || vote.length > NS2G.voteRules[idx].getVotesLimit()) {
                if (removeVotes(getSteamId())) {
                    postVoteChangeMessage();
                    postMessage(MessageType.USER_UNREADY, getUserName());
                }
                throw LogicExceptionFormatted.format("Ожидается %s голосов за %s, получено %d. Пожалуйста, переголосуйте.",
                        NS2G.voteRules[idx].voteRange(), NS2G.voteRules[idx].getName(), vote.length);
            }
            idx++;
        }
    }

    private Boolean removeVotes(final Long steamId) throws LogicException, ClientAuthException {
        return HibernateUtil.exec(new HibernateCallback<Boolean>() {

            @Override
            public Boolean run(Session session) throws LogicException, ClientAuthException {
                @SuppressWarnings("unchecked")
                List<PlayerVote> playerVotes = session.createQuery("from PlayerVote pv where pv.steamId = :sid").setLong("sid", steamId)
                        .list();
                for (PlayerVote playerVote : playerVotes) {
                    session.delete(playerVote);
                }
                return playerVotes.size() > 0;
            }
        });
    }

    private List<VoteResult> getVoteResultsByType(final Long gatherId, Session session, VoteType voteType) throws LogicException,
            ClientAuthException {
        int idx = voteType.ordinal();
        session.enableFilter("gatherId").setParameter("gid", gatherId);
        @SuppressWarnings("unchecked")
        List<VoteResult> voteResults = session
                .createQuery(
                        "select vr from VoteResult vr left join vr.gather where vr.type = :vt order by vr.voteCount desc, vr.place, rand()")
                .setParameter("vt", voteType).setMaxResults(NS2G.voteRules[idx].getWinnerCount()).list();
        if (voteResults.size() < NS2G.voteRules[idx].getWinnerCount()) {
            throw LogicExceptionFormatted.format("Невозможно определить %s, переголосовка.", NS2G.voteRules[idx].getName());
        }
        return voteResults;
    }

    @Override
    public void fakeLogin() throws ClientAuthException, LogicException {
        requiresDebug();
        getSession().removeAttribute(Settings.GATHER_ID);
        getSession().setAttribute(Settings.STEAMID_SESSION, new Random().nextLong());
    }

    private void requiresDebug() throws ClientAuthException {
        if (!debug) {
            throw new ClientAuthException("debug mode required");
        }
    }

}
