package me.rkfg.ns2gather.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import me.rkfg.ns2gather.client.AnonymousAuthException;
import me.rkfg.ns2gather.client.ClientSettings;
import me.rkfg.ns2gather.client.NS2GService;
import me.rkfg.ns2gather.domain.Gather;
import me.rkfg.ns2gather.domain.Map;
import me.rkfg.ns2gather.domain.Player;
import me.rkfg.ns2gather.domain.PlayerVote;
import me.rkfg.ns2gather.domain.Remembered;
import me.rkfg.ns2gather.domain.Server;
import me.rkfg.ns2gather.domain.Streamer;
import me.rkfg.ns2gather.domain.Vote;
import me.rkfg.ns2gather.domain.VoteResult;
import me.rkfg.ns2gather.dto.GatherState;
import me.rkfg.ns2gather.dto.HiveStatsDTO;
import me.rkfg.ns2gather.dto.InitStateDTO;
import me.rkfg.ns2gather.dto.MapDTO;
import me.rkfg.ns2gather.dto.MessageDTO;
import me.rkfg.ns2gather.dto.MessageType;
import me.rkfg.ns2gather.dto.MessageVisibility;
import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.Side;
import me.rkfg.ns2gather.dto.VoteResultDTO;
import me.rkfg.ns2gather.dto.VoteType;
import me.rkfg.ns2gather.server.GatherPlayersManager.CleanupCallback;
import me.rkfg.ns2gather.server.GatherPlayersManager.GatherPlayers;
import me.rkfg.ns2gather.server.GatherPlayersManager.TeamStatType;
import me.rkfg.ns2gather.server.ServerManager.ServersChangeCallback;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Session;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.ClientAuthenticationException;
import ru.ppsrk.gwt.client.LogicException;
import ru.ppsrk.gwt.server.HibernateCallback;
import ru.ppsrk.gwt.server.HibernateUtil;
import ru.ppsrk.gwt.server.LogicExceptionFormatted;
import ru.ppsrk.gwt.server.LongPollingServer;
import ru.ppsrk.gwt.server.ServerUtils;

import com.google.gwt.user.client.rpc.AsyncCallback;
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
    Logger logger = LoggerFactory.getLogger(getClass());

    CleanupManager cleanupManager = new CleanupManager();
    static ConsumerManager consumerManager = new ConsumerManager();
    ServerManager serverManager;
    GatherPlayersManager connectedPlayers = new GatherPlayersManager(new CleanupCallback() {

        @Override
        public void playerRemoved(Long gatherId, Long steamId) throws LogicException, ClientAuthException {
            removePlayer(gatherId, steamId, false);
        }
    });

    Long playerId = 1L;
    MessageManager messageManager = new MessageManager();
    GatherCountdownManager gatherCountdownManager = new GatherCountdownManager();
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
            Long gatherId = null;
            Long steamId = null;
            try {
                gatherId = getCurrentGatherId();
                steamId = getSteamId();
            } catch (ClientAuthException e) {
            }
            result = messageManager.getNewMessages(gatherId, steamId, since);
            if (result.isEmpty()) {
                return null;
            }
            return result;
        }

        @Override
        public void close() throws Exception {
            logger.info("Stopping the long polling " + Thread.currentThread());
            super.close();
        }
    }

    public NS2GServiceImpl() {
        ServerUtils.setMappingFile("dozerMapping.xml");
        debug = HibernateUtil.initSessionFactoryDebugRelease(forceDebug, forceRelease, "hibernate_dev.cfg.xml", "hibernate.cfg.xml");
        try {
            resetVotes(null, ResetType.ALL);
        } catch (LogicException | ClientAuthException e) {
            e.printStackTrace();
        }
        serverManager = new ServerManager(new ServersChangeCallback() {

            @Override
            public void onChange() {
                try {
                    messageManager.postMessage(MessageType.SERVER_UPDATE, "", null);
                } catch (LogicException | ClientAuthException e) {
                    e.printStackTrace();
                }
            }
        });
        cleanupManager.add(connectedPlayers);
        cleanupManager.add(messageManager);
        cleanupManager.add(serverManager);
    }

    protected void removeVotesForPlayer(final Long gatherId, final Long playerLeftId) throws LogicException, ClientAuthException {
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
            List<Long> votes = HibernateUtil.exec(new HibernateCallback<List<Long>>() {

                @SuppressWarnings("unchecked")
                @Override
                public List<Long> run(Session session) throws LogicException, ClientAuthException {
                    session.enableFilter("gatherId").setParameter("gid", gatherId);
                    return session
                            .createQuery(
                                    "select pv.steamId from PlayerVote pv, Vote v, Gather g2 where v in elements (pv.votes) and v.gather = g2 and v.type = :comm and v.targetId = :tid")
                            .setParameter("comm", VoteType.COMM).setLong("tid", playerLeftId).list();
                }
            });
            for (Long steamId : votes) {
                MessageDTO messageDTO = new MessageDTO(MessageType.VOTE_ENDED,
                        "Игрок, за которого вы голосовали, ушёл. Пожалуйста, переголосуйте.", gatherId);
                messageDTO.setVisibility(MessageVisibility.PERSONAL);
                messageDTO.setToSteamId(steamId);
                messageManager.postMessage(messageDTO);
                unvote(gatherId, steamId);
            }
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
                            "select pv from PlayerVote pv left join pv.votes v left join v.gather g, Gather g2 where g = g2").list();
                    for (PlayerVote playerVote : playerVotes) {
                        session.delete(playerVote);
                    }
                    messageManager.postMessage(MessageType.RESET_HIGHLIGHT, "", gatherId);
                }
                if (type == ResetType.ALL || type == ResetType.RESULTS) {
                    @SuppressWarnings("unchecked")
                    List<VoteResult> voteResults = session.createQuery(
                            "select vr from VoteResult vr left join vr.gather g, Gather g2 where g = g2").list();
                    for (VoteResult voteResult : voteResults) {
                        session.delete(voteResult);
                    }
                }
                return null;
            }
        });
    }

    @Override
    public String login() throws LogicException {
        try {
            // perform discovery on the user-supplied identifier
            List<?> discoveries = consumerManager.discover("http://steamcommunity.com/openid");

            // attempt to associate with the OpenID provider
            // and retrieve one service endpoint for authentication
            DiscoveryInformation discovered = consumerManager.associate(discoveries);

            // store the discovery information in the user's session for later use
            // leave out for stateless operation / if there is no session
            getSession().setAttribute("discovered", discovered);

            // obtain a AuthRequest message to be sent to the OpenID provider
            AuthRequest authReq = consumerManager.authenticate(discovered, Settings.CALLBACK_URL);
            String result = authReq.getDestinationUrl(true);
            return result;
        } catch (MessageException | ConsumerException | DiscoveryException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Long getSteamId() throws ClientAuthException, LogicException {
        Boolean isAnonymous = (Boolean) getSession().getAttribute(Settings.ANONYMOUS_SESSION);
        if (isAnonymous != null) {
            throw new AnonymousAuthException("anonymous");
        }
        Long result = (Long) getSession().getAttribute(Settings.STEAMID_SESSION);
        if (result == null) {
            result = rememberMe();
            if (result == null) {
                throw new ClientAuthenticationException("not logged in");
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
                        AuthCallbackServlet.updateRememberCookie(getThreadLocalResponse(), rid.toString());
                    } catch (NonUniqueResultException e) {
                        throw new LogicException("Дублирующийся id в БД, автовход отклонён.");
                    }
                }
                return steamId;
            }
        });
    }

    @Override
    public PlayerDTO getPlayer() throws LogicException, ClientAuthException {
        return getPlayer(getSteamId());
    }

    public PlayerDTO getPlayer(Long steamId) throws LogicException, ClientAuthException {
        PlayerDTO player = connectedPlayers.getPlayerBySteamId(steamId);
        if (player != null) {
            return player;
        }
        if (debug) {
            player = new PlayerDTO(steamId, "fake" + steamId.toString(), "http://steamcommunity.com", System.currentTimeMillis());
            connectedPlayers.addPlayerBySteamId(player);
        } else {
            player = connectedPlayers.lookupPlayerBySteamId(steamId);
        }
        return player;
    }

    @Override
    public List<ServerDTO> getServers() throws LogicException, ClientAuthException {
        return serverManager.getServers();
    }

    @Override
    public List<MapDTO> getMaps() throws LogicException, ClientAuthException {
        return HibernateUtil.queryList("from Map", new String[] {}, new Object[] {}, MapDTO.class);
    }

    @Override
    public void ping() throws ClientAuthException, LogicException {
        synchronized (connectedPlayers.getPlayersByGather(getCurrentGatherId())) {
            Long steamId = getSteamId();
            // this should be inside synchronized block to prevent racing with kick
            final Long gatherId = getCurrentGatherId();
            PlayerDTO existing = connectedPlayers.getPlayerByGatherSteamId(gatherId, steamId);
            if (existing == null) {
                existing = connectedPlayers.getPlayerBySteamId(steamId);
                if (existing == null) {
                    return;
                }
                existing.setLastPing(System.currentTimeMillis());
                connectedPlayers.addPlayerToGather(gatherId, existing);
                if (System.currentTimeMillis() - existing.getLastHiveUpdate() > Settings.HIVE_UPDATE_INTERVAL) {
                    final PlayerDTO hivePlayer = existing;
                    connectedPlayers.getPlayersByGather(gatherId).getHiveStats(steamId, new AsyncCallback<HiveStatsDTO>() {

                        @Override
                        public void onSuccess(HiveStatsDTO result) {
                            try {
                                logger.info("Got hive info for {}", hivePlayer.getId());
                                hivePlayer.setLastHiveUpdate(System.currentTimeMillis());
                                messageManager.postMessage(MessageType.PLAYERS_UPDATE, "", gatherId);
                            } catch (LogicException | ClientAuthException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            logger.warn("Can't get hive info for {}", hivePlayer.getId());
                            hivePlayer.setLastHiveUpdate(System.currentTimeMillis() + Settings.HIVE_UPDATE_FAILURE_WAIT
                                    - Settings.HIVE_UPDATE_INTERVAL);
                        }
                    });
                }
                messageManager.postMessage(MessageType.USER_ENTERS, existing.getEffectiveName(), gatherId);
                postVoteChangeMessage(gatherId);
                updateGatherStateByPlayerNumber(gatherId);
            } else {
                existing.setLastPing(System.currentTimeMillis());
            }
        }
    }

    private void updateGatherStateByPlayerNumber(final Long gatherId) throws LogicException, ClientAuthException {
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
            Gather gather = getGatherById(gatherId);
            if (isGatherClosed(gatherId)) {
                // nothing to do here
                return;
            }
            int connectedPlayersCount = connectedPlayers.getPlayersByGather(gatherId).playerCount();
            if (connectedPlayersCount == Settings.GATHER_PLAYER_MAX) {
                if (gather.getState() == GatherState.OPEN) {
                    // close gather if player limit reached
                    updateGatherState(gatherId, GatherState.CLOSED);
                }
            } else {
                if (gather.getState() == GatherState.CLOSED) {
                    // reopen gather if players have left
                    updateGatherState(gatherId, GatherState.OPEN);
                }
            }
            if (connectedPlayersCount >= Settings.GATHER_PLAYER_MIN) {
                // minimum players count reached
                if (getVotedPlayersCount(gatherId) < connectedPlayersCount) {
                    // not everyone voted yet, check for evenness
                    if (connectedPlayersCount % 2 == 0) {
                        // even number, run the timer
                        gatherCountdownManager.scheduleGatherCountdownTask(gatherId, new TimerTask() {

                            @Override
                            public void run() {
                                try {
                                    resolveGather(gatherId);
                                } catch (LogicException | ClientAuthException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }, Settings.GATHER_RESOLVE_DELAY);
                        updateGatherState(gatherId, GatherState.ONTIMER);
                        messageManager.postMessage(MessageType.RUN_TIMER, String.valueOf(Settings.GATHER_RESOLVE_DELAY / 1000), gatherId);
                    } else {
                        // odd number, stop the timer and wait
                        stopGatherTimer(gatherId);
                        messageManager.postMessage(MessageType.MORE_PLAYERS, "", gatherId);
                    }
                } else {
                    // gather is fully ready, let's count votes
                    countResults(gatherId);
                }
            } else {
                stopGatherTimer(gatherId);
            }
        }
    }

    private void stopGatherTimer(final Long gatherId) throws LogicException, ClientAuthException {
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
            messageManager.postMessage(MessageType.STOP_TIMER, "", gatherId);
            gatherCountdownManager.cancelGatherCountdownTasks(gatherId);
            if (getGatherById(gatherId).getState() == GatherState.ONTIMER) {
                updateGatherState(gatherId, GatherState.OPEN);
            }
        }
    }

    protected void resolveGather(final Long gatherId) throws LogicException, ClientAuthException {
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
            HibernateUtil.exec(new HibernateCallback<Void>() {

                @Override
                public Void run(Session session) throws LogicException, ClientAuthException {
                    session.enableFilter("gatherId").setParameter("gid", gatherId);
                    @SuppressWarnings("unchecked")
                    Set<Long> votedSteamIds = new HashSet<>(session.createQuery(
                            "select distinct(v.player.steamId) from Vote v left join v.gather").list());
                    GatherPlayers gatherPlayers = connectedPlayers.getPlayersByGather(gatherId);
                    for (PlayerDTO player : gatherPlayers.getPlayers()) {
                        if (!votedSteamIds.contains(player.getId())) {
                            removePlayer(gatherId, player.getId(), true);
                        }
                    }
                    return null;
                }
            });
            updateGatherStateByPlayerNumber(gatherId);
        }
    }

    @Override
    public void unvote() throws LogicException, ClientAuthException {
        requiresOngoingGather();
        unvote(getCurrentGatherId(), getSteamId());
    }

    private void unvote(Long gatherId, Long steamId) throws LogicException, ClientAuthException {
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
            if (removeVotes(steamId)) {
                sendReadiness(getPlayer(steamId).getId(), gatherId, false);
            }
        }
    }

    private void updateGatherState(Long gatherId, GatherState newState) throws LogicException, ClientAuthException {
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
            Gather gather = getGatherById(gatherId);
            gather.setState(newState);
            messageManager.postMessage(MessageType.GATHER_STATUS, String.valueOf(newState.ordinal()), gather.getId());
            HibernateUtil.saveObject(gather);
        }
    }

    @Override
    public List<MessageDTO> getNewMessages(Long since) {
        MessagePollingServer server = null;
        try {
            if (since < 0) {
                since += System.currentTimeMillis();
            }
            server = new MessagePollingServer(30000, 100, since);
            cleanupManager.add(server);
            return server.start();
        } catch (InterruptedException | LogicException | ClientAuthException e) {
            return null;
        } finally {
            cleanupManager.remove(server);
        }
    }

    @Override
    public void sendChatMessage(String text) throws ClientAuthException, LogicException {
        if (text.length() > ClientSettings.CHAT_MAX_LENGTH) {
            text = text.substring(0, ClientSettings.CHAT_MAX_LENGTH);
        }
        messageManager.postMessage(MessageType.CHAT_MESSAGE, getPlayer().getEffectiveName() + ": " + text, getCurrentGatherId());
    }

    @Override
    public List<PlayerDTO> getConnectedPlayers() throws LogicException, ClientAuthException {
        List<PlayerDTO> result = new LinkedList<>();
        result.addAll(connectedPlayers.getPlayersByGather(getCurrentGatherId()).getPlayers());
        Collections.sort(result, new Comparator<PlayerDTO>() {

            @Override
            public int compare(PlayerDTO o1, PlayerDTO o2) {
                return o1.getEffectiveName().compareTo(o2.getEffectiveName());
            }
        });
        return result;
    }

    @Override
    public void vote(final Long[][] votes) throws LogicException, ClientAuthException {
        Long gatherId = getCurrentGatherId();
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
            requiresOngoingGather();
            if (votes.length != 3) {
                throw LogicExceptionFormatted.format("invalid vote size, expected %d, got %d", 3, votes.length);
            }
            validateVoteNumbers(votes);
            boolean showVote = HibernateUtil.exec(new HibernateCallback<Boolean>() {

                @Override
                public Boolean run(Session session) throws LogicException, ClientAuthException {
                    Long steamId = getSteamId();
                    // delete all previous votes
                    boolean showVote = !removeVotes(steamId);
                    PlayerVote playerVote = new PlayerVote(steamId);
                    for (int i = 0; i < 3; i++) {
                        Long[] votesCat = votes[i];
                        for (Long targetId : votesCat) {
                            Vote vote = new Vote(playerVote, targetId, VoteType.values()[i], getCurrentGather());
                            playerVote.getVotes().add(vote);
                        }
                    }
                    session.merge(playerVote);
                    return showVote;
                }
            });
            if (showVote) {
                sendReadiness(getPlayer().getId(), gatherId, true);
            }
            int connectedPlayersCount = connectedPlayers.getPlayersByGather(gatherId).playerCount();
            if (connectedPlayersCount >= Settings.GATHER_PLAYER_MIN && getVotedPlayersCount(gatherId) == connectedPlayersCount) {
                countResults(gatherId);
            }
        }
    }

    private void requiresOngoingGather() throws LogicException, ClientAuthException {
        if (isGatherClosed(getCurrentGatherId())) {
            throw new LogicException("Голосование уже завершено.");
        }
    }

    private boolean isGatherClosed(Long gatherId) throws LogicException, ClientAuthException {
        return Arrays.asList(GatherState.COMPLETED, GatherState.SIDEPICK, GatherState.PLAYERS).contains(getGatherById(gatherId).getState());
    }

    protected Gather getCurrentGather() throws LogicException, ClientAuthException {
        return getGatherById(getCurrentGatherId());
    }

    @Override
    public List<PlayerDTO> getGatherParticipantsList() throws LogicException, ClientAuthException {
        List<PlayerDTO> participantList = new ArrayList<>(connectedPlayers.getPlayersByGather(getCurrentGatherId()).getParticipants());
        Collections.sort(participantList, new Comparator<PlayerDTO>() {

            @Override
            public int compare(PlayerDTO o1, PlayerDTO o2) {
                return o1.getLastPing().compareTo(o2.getLastPing());
            }
        });
        return participantList;
    }

    protected Gather getGatherById(Long gatherId) throws LogicException, ClientAuthException {
        return HibernateUtil.tryGetObject(gatherId, Gather.class, "Не удалось найти gather id=" + gatherId
                + " в БД. Операция не выполнена.");
    }

    private void countResults(final Long gatherId) throws LogicException, ClientAuthException {
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
            stopGatherTimer(gatherId);
            try {
                HibernateUtil.exec(new HibernateCallback<Void>() {

                    @Override
                    public Void run(Session session) throws LogicException, ClientAuthException {
                        session.enableFilter("gatherId").setParameter("gid", gatherId);
                        resetVotes(gatherId, ResetType.RESULTS);
                        Gather gather = getGatherById(gatherId);
                        for (VoteType voteType : VoteType.values()) {
                            @SuppressWarnings("unchecked")
                            List<Vote> votesForType = session
                                    .createQuery("select v from Vote v left join v.gather g, Gather g2 where v.type = :vt and g = g2")
                                    .setParameter("vt", voteType).list();
                            for (Vote vote : votesForType) {
                                Long targetId = vote.getTargetId();
                                try {
                                    VoteResult existingVoteResult = (VoteResult) session
                                            .createQuery(
                                                    "select vr from VoteResult vr left join vr.gather g, Gather g2 where vr.type = :vt and vr.targetId = :tid and g = g2")
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
                                            "select vr from VoteResult vr left join vr.gather g, Gather g2 where vr.type = :vt and vr not in (:vrl) and g = g2")
                                    .setParameter("vt", voteType).setParameterList("vrl", results).list();
                            for (VoteResult voteResult : toCrop) {
                                session.delete(voteResult);
                            }
                        }
                        List<Long> commsId = new ArrayList<Long>();
                        for (VoteResult voteResult : getVoteResultsByType(gatherId, session, VoteType.COMM)) {
                            commsId.add(voteResult.getTargetId());
                        }
                        GatherPlayers players = connectedPlayers.getPlayersByGather(gatherId);
                        players.setComms(commsId);
                        players.playersToParticipants();
                        setupServer(gatherId, session);
                        return null;
                    }
                });
            } catch (LogicException e) {
                resetVotes(gatherId, ResetType.ALL);
                messageManager.postMessage(MessageType.VOTE_ENDED, e.getMessage(), gatherId);
                postVoteChangeMessage(gatherId);
                updateGatherStateByPlayerNumber(gatherId);
                return;
            }
            resetVotes(gatherId, ResetType.VOTES);
            updateGatherState(gatherId, GatherState.SIDEPICK);
            messageManager.postMessage(MessageType.VOTE_ENDED, "ok", gatherId);
        }
    }

    private void setupServer(Long gatherId, Session session) throws LogicException, ClientAuthException {
        List<VoteResult> servers = getVoteResultsByType(gatherId, session, VoteType.SERVER);
        if (servers.size() == 0) {
            throw new LogicException("Серверы не выбраны, невозможно сменить пароль и карту.");
        }
        List<VoteResult> maps = getVoteResultsByType(gatherId, session, VoteType.MAP);
        if (maps.size() == 0) {
            throw new LogicException("Карты не выбраны, невозможно сменить пароль и карту.");
        }
        Long serverId = servers.get(0).getTargetId();
        Server server = HibernateUtil.tryGetObject(serverId, Server.class, session, "Выбранный сервер с id " + serverId
                + " не найден в БД.");
        if (server.getWebLogin() == null || server.getWebPassword() == null || server.getWebPort() == null) {
            return;
        }
        Long mapId = maps.get(0).getTargetId();
        Map map = HibernateUtil.tryGetObject(mapId, Map.class, session, "Выбранная карта с id " + mapId + " не найдена в БД.");
        server.setPassword(genPassword(Settings.SERVER_PASSWORD_LENGTH));
        HttpClient webClient = NetworkUtils.getHTTPClient(server.getIp(), server.getWebPort(), server.getWebLogin(),
                server.getWebPassword());
        try {
            HttpUriRequest changePasswordRequest = buildWebAdminGetRequest(server, "sv_password+" + server.getPassword());
            webClient.execute(changePasswordRequest);
        } catch (IOException e) {
            throw new LogicException("Не удалось сменить пароль на сервере " + server.getName());
        }
        try {
            HttpUriRequest changeMapRequest = buildWebAdminGetRequest(server, "sv_changemap+" + map.getName());
            webClient.execute(changeMapRequest);
        } catch (IOException e) {
            throw new LogicException("Не удалось сменить карту на сервере " + server.getName());
        }
    }

    private HttpUriRequest buildWebAdminGetRequest(Server server, String command) {
        return new HttpGet("http://" + server.getIp() + ":" + server.getWebPort() + "/?command=Send&rcon=" + command);
    }

    private String genPassword(int length) {
        Random random = new Random();
        String chars = "123456789qwertyuiopasdfghkzxcvbnm";
        StringBuilder result = new StringBuilder(length);
        for (; length > 0; length--) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }
        return result.toString();
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
                        result.add(voteResult.toDTO(session, connectedPlayers.getPlayersByGather(gatherId)));
                    }
                }
                return result;
            }
        });
    }

    protected Long getCurrentGatherId() throws LogicException, ClientAuthException {
        try {
            String kickReason = connectedPlayers.getKickedReason(getSteamId());
            if (kickReason != null) {
                throw new ClientAuthenticationException(kickReason);
            }
        } catch (AnonymousAuthException e) {
            // ok, let the anon pass in
        }
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
        synchronized (connectedPlayers) {
            return HibernateUtil.exec(new HibernateCallback<Long>() {

                @Override
                public Long run(Session session) throws LogicException, ClientAuthException {
                    Gather gather = (Gather) session.createQuery("from Gather g where g.state in (:states)")
                            .setParameterList("states", Arrays.asList(GatherState.OPEN, GatherState.ONTIMER)).setMaxResults(1)
                            .uniqueResult();
                    if (gather == null) {
                        // create new gather
                        gather = (Gather) session.merge(new Gather());
                    }
                    return gather.getId();
                }
            });
        }
    }

    private void postVoteChangeMessage(Long gatherId) throws LogicException, ClientAuthException {
        messageManager.postMessage(MessageType.VOTE_CHANGE, getVoteStat(gatherId), gatherId);
    }

    @Override
    public String getVoteStat() throws LogicException, ClientAuthException {
        return getVoteStat(getCurrentGatherId());
    }

    private String getVoteStat(Long gatherId) throws LogicException, ClientAuthException {
        return String.format("%d/%d", getVotedPlayersCount(gatherId), connectedPlayers.getPlayersByGather(gatherId).playerCount());
    }

    private Long getVotedPlayersCount(final Long gatherId) throws LogicException, ClientAuthException {
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
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
    }

    private void validateVoteNumbers(final Long[][] votes) throws LogicException, ClientAuthException {
        try {
            HibernateUtil.exec(new HibernateCallback<Void>() {
                @Override
                public Void run(Session session) throws LogicException, ClientAuthException {
                    int idx = 0;
                    Long gatherId = getCurrentGatherId();
                    for (Long[] vote : votes) {
                        if (vote.length < ClientSettings.voteRules[idx].getVotesRequired()
                                || vote.length > ClientSettings.voteRules[idx].getVotesLimit()) {
                            throw LogicExceptionFormatted.format("Ожидается %s голосов за %s, получено %d. Пожалуйста, переголосуйте.",
                                    ClientSettings.voteRules[idx].voteRange(), ClientSettings.voteRules[idx].getName(), vote.length);
                        }
                        for (Long targetId : vote) {
                            VoteResult.getTarget(session, connectedPlayers.getPlayersByGather(gatherId), VoteType.values()[idx], targetId);
                        }
                        idx++;
                    }
                    return null;
                }
            });
        } catch (LogicException e) {
            if (removeVotes(getSteamId())) {
                sendReadiness(getPlayer().getId(), getCurrentGatherId(), false);
            }
            throw e;
        }
    }

    private void sendReadiness(Long steamId, Long gatherId, boolean ready) throws LogicException, ClientAuthException {
        postVoteChangeMessage(gatherId);
        messageManager.postMessage(ready ? MessageType.USER_READY : MessageType.USER_UNREADY, steamId.toString(), gatherId);
    }

    private Boolean removeVotes(final Long steamId) throws LogicException, ClientAuthException {
        synchronized (connectedPlayers) {
            return HibernateUtil.exec(new HibernateCallback<Boolean>() {

                @Override
                public Boolean run(Session session) throws LogicException, ClientAuthException {
                    @SuppressWarnings("unchecked")
                    List<PlayerVote> playerVotes = session.createQuery("from PlayerVote pv where pv.steamId = :sid")
                            .setLong("sid", steamId).list();
                    for (PlayerVote playerVote : playerVotes) {
                        session.delete(playerVote);
                    }
                    return playerVotes.size() > 0;
                }
            });
        }
    }

    private List<VoteResult> getVoteResultsByType(final Long gatherId, Session session, VoteType voteType) throws LogicException,
            ClientAuthException {
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
            int idx = voteType.ordinal();
            session.enableFilter("gatherId").setParameter("gid", gatherId);
            @SuppressWarnings("unchecked")
            List<VoteResult> voteResults = session
                    .createQuery(
                            "select vr from VoteResult vr left join vr.gather g, Gather g2 where vr.type = :vt and g = g2 order by vr.voteCount desc, vr.place, rand()")
                    .setParameter("vt", voteType).setMaxResults(ClientSettings.voteRules[idx].getWinnerCount()).list();
            if (voteResults.size() < ClientSettings.voteRules[idx].getWinnerCount()) {
                throw LogicExceptionFormatted.format("Невозможно определить %s, переголосовка.", ClientSettings.voteRules[idx].getName());
            }
            return voteResults;
        }
    }

    @Override
    public void fakeLogin() throws ClientAuthException, LogicException {
        requiresDebug();
        getSession().removeAttribute(Settings.GATHER_ID);
        getSession().setAttribute(Settings.STEAMID_SESSION, new Random().nextLong());
    }

    private void requiresDebug() throws LogicException {
        if (!debug) {
            throw new LogicException("debug mode required");
        }
    }

    @Override
    public void logout() throws LogicException, ClientAuthException {
        HibernateUtil.exec(new HibernateCallback<Void>() {

            @Override
            public Void run(Session session) throws LogicException, ClientAuthException {
                try {
                    Long steamId = getSteamId();
                    @SuppressWarnings("unchecked")
                    List<Remembered> remembereds = session.createQuery("from Remembered r where r.steamId = :sid").setLong("sid", steamId)
                            .list();
                    for (Remembered remembered : remembereds) {
                        session.delete(remembered);
                    }
                    removePlayer(getCurrentGatherId(), steamId, false);
                } catch (AnonymousAuthException e) {

                }
                getSession().invalidate();
                return null;
            }
        });
    }

    @Override
    public void resetGatherPresence() throws LogicException, ClientAuthException {
        connectedPlayers.removeKickedId(getSteamId());
        getSession().removeAttribute(Settings.GATHER_ID);
    }

    @Override
    public Set<Long> getVotedPlayerIds() throws LogicException, ClientAuthException {
        return HibernateUtil.exec(new HibernateCallback<Set<Long>>() {

            @Override
            public Set<Long> run(Session session) throws LogicException, ClientAuthException {
                Set<Long> result = new HashSet<>();
                Long gatherId = getCurrentGatherId();
                session.enableFilter("gatherId").setParameter("gid", gatherId);
                @SuppressWarnings("unchecked")
                List<Long> votedSteamIds = session.createQuery(
                        "select distinct(v.player.steamId) from Vote v left join v.gather g, Gather g2 where g = g2").list();
                GatherPlayers gatherPlayers = connectedPlayers.getPlayersByGather(gatherId);
                for (Long voteSteamId : votedSteamIds) {
                    PlayerDTO playerDTO = gatherPlayers.getPlayer(voteSteamId);
                    if (playerDTO != null) {
                        result.add(playerDTO.getId());
                    }
                }
                return result;
            }
        });
    }

    @Override
    public GatherState getGatherState() throws LogicException, ClientAuthException {
        return getCurrentGather().getState();
    }

    @Override
    public InitStateDTO getInitState() throws LogicException, ClientAuthException {
        try {
            getCurrentGatherId();
        } catch (ClientAuthException e) {
            throw new LogicException(e.getMessage());
        }
        InitStateDTO result = new InitStateDTO();
        result.setGatherState(getGatherState());
        result.setMaps(getMaps());
        result.setPlayers(getConnectedPlayers());
        result.setServers(getServers());
        result.setVotedIds(getVotedPlayerIds());
        result.setVoteStat(getVoteStat());
        result.setVersion(getVersion());
        try {
            result.setPasswords(getPasswordsForStreamer());
        } catch (AnonymousAuthException e) {

        }
        if (isGatherClosed(getCurrentGatherId())) {
            result.setVoteResults(getVoteResults());
        }
        return result;
    }

    private String getPasswordsForStreamer() throws LogicException, ClientAuthException {
        return HibernateUtil.exec(new HibernateCallback<String>() {
            @Override
            public String run(Session session) throws LogicException, ClientAuthException {
                @SuppressWarnings("unchecked")
                List<Streamer> streamers = session.createQuery("from Streamer s where s.steamId = :sid").setLong("sid", getSteamId())
                        .list();
                if (streamers.size() == 0) {
                    return null;
                }
                StringBuilder result = new StringBuilder();
                result.append("Вы являетесь стримером [").append(streamers.get(0).getName())
                        .append("]. Данные для подключения к серверам:<br/>");
                @SuppressWarnings("unchecked")
                List<Server> servers = session.createQuery("from Server s").list();
                for (Server server : servers) {
                    result.append("Сервер: ").append(server.getName()).append("; ");
                    String password = server.getPassword();
                    if (password != null && !password.isEmpty()) {
                        result.append("пароль: ").append(server.getPassword());
                    } else {
                        result.append("пароля нет");
                    }
                    result.append("<br/>");
                }
                return result.toString();
            }
        });
    }

    private String getVersion() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream("/buildNumber.properties"));
            return "build " + properties.getProperty("version", "no version");
        } catch (IOException e) {
            return "no version";
        }
    }

    private void removePlayer(Long gatherId, Long steamId, boolean isKicked) throws LogicException, ClientAuthException {
        synchronized (connectedPlayers.getPlayersByGather(gatherId)) {
            logger.info("Removing votes...");
            removeVotes(steamId);
            logger.info("Removing votes for player...");
            removeVotesForPlayer(gatherId, steamId);
            logger.info("Removing player from gather...");
            connectedPlayers.removePlayerFromGather(gatherId, steamId);
            logger.info("Posting message...");
            messageManager.postMessage(MessageType.USER_LEAVES, steamId.toString(), gatherId);
            logger.info("Is kicked?");
            if (isKicked) {
                logger.info("Yep, kicked?");
                connectedPlayers.addKicked(steamId,
                        "Вы были кикнуты из Gather по неактивности. Нажмите «Зайти в новый сбор», чтобы продолжить участие.");
                logger.info("Notifying about kick...");
                messageManager.postMessage(MessageDTO.privateMessage(steamId, MessageType.USER_KICKED, "", null));
            }
            logger.info("Notifying about votechange...");
            postVoteChangeMessage(gatherId);
            logger.info("Updating gather state...");
            updateGatherStateByPlayerNumber(gatherId);
        }
    }

    @Override
    public void pickSide(final Side side) throws LogicException, ClientAuthException {
        Long gatherId = getCurrentGatherId();
        requiresGatherState(gatherId, GatherState.SIDEPICK, "Сейчас нельзя выбирать строну.");
        connectedPlayers.getPlayersByGather(gatherId).pickSide(getSteamId(), side);
        messageManager.postMessage(MessageType.PICKED, "", gatherId);
        updateGatherState(gatherId, GatherState.PLAYERS);
    }

    private void requiresGatherState(Long gatherId, GatherState state, String errorMessage) throws LogicException, ClientAuthException {
        if (getGatherById(gatherId).getState() != state) {
            throw new LogicException(errorMessage);
        }
    }

    @Override
    public void pickPlayer(Long playerSteamId) throws LogicException, ClientAuthException {
        Long gatherId = getCurrentGatherId();
        requiresGatherState(gatherId, GatherState.PLAYERS, "Сейчас нельзя выбирать игроков.");
        GatherPlayers gatherPlayers = connectedPlayers.getPlayersByGather(gatherId);
        gatherPlayers.pickPlayer(getSteamId(), playerSteamId);
        messageManager.postMessage(MessageType.PICKED, "", gatherId);
        if (gatherPlayers.getTeamsStat(TeamStatType.NOFREE)) {
            updateGatherState(gatherId, GatherState.COMPLETED);
        }
    }

    @Override
    public void destroy() {
        try {
            cleanupManager.doCleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        HibernateUtil.cleanup();
        HibernateUtil.mysqlCleanup();
        logger.info("Cleanup complete. Good bye");
    }

    @Override
    public void loginAnonymously() throws LogicException, ClientAuthException {
        getSession().setAttribute(Settings.ANONYMOUS_SESSION, true);
        getCurrentGatherId();
    }

    @Override
    public void changeNick(String newNick) throws LogicException, ClientAuthException {
        Long gatherId = getCurrentGatherId();
        changeNick(connectedPlayers.getPlayerByGatherSteamId(gatherId, getSteamId()), newNick);
        messageManager.postMessage(MessageType.PLAYERS_UPDATE, "", gatherId);
    }

    private void changeNick(final PlayerDTO playerDTO, final String newNick) throws LogicException, ClientAuthException {
        HibernateUtil.exec(new HibernateCallback<Void>() {

            @Override
            public Void run(Session session) throws LogicException, ClientAuthException {
                Player playerEnt = (Player) session.createQuery("from Player p where p.steamId = :sid").setLong("sid", playerDTO.getId())
                        .uniqueResult();
                if (playerEnt == null) {
                    session.merge(new Player(newNick, playerDTO.getId()));
                } else {
                    playerEnt.setNick(newNick);
                }
                playerDTO.setNick(newNick);
                return null;
            }
        });
    }
}
