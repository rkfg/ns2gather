package me.rkfg.ns2gather.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import me.rkfg.ns2gather.client.ClientSettings;
import me.rkfg.ns2gather.client.NS2GService;
import me.rkfg.ns2gather.domain.Gather;
import me.rkfg.ns2gather.domain.PlayerVote;
import me.rkfg.ns2gather.domain.Remembered;
import me.rkfg.ns2gather.domain.Vote;
import me.rkfg.ns2gather.domain.VoteResult;
import me.rkfg.ns2gather.domain.VoteType;
import me.rkfg.ns2gather.dto.GatherState;
import me.rkfg.ns2gather.dto.InitStateDTO;
import me.rkfg.ns2gather.dto.MapDTO;
import me.rkfg.ns2gather.dto.MessageDTO;
import me.rkfg.ns2gather.dto.MessageType;
import me.rkfg.ns2gather.dto.MessageVisibility;
import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.VoteResultDTO;
import me.rkfg.ns2gather.server.GatherPlayersManager.CleanupCallback;
import me.rkfg.ns2gather.server.GatherPlayersManager.GatherPlayers;

import org.hibernate.NonUniqueResultException;
import org.hibernate.Session;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.MessageException;

import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.ClientAuthenticationException;
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
    GatherPlayersManager connectedPlayers = new GatherPlayersManager(new CleanupCallback() {

        @Override
        public void playerRemoved(Long gatherId, PlayerDTO player) throws LogicException, ClientAuthException {
            removePlayer(gatherId, player.getId(), false);
        }
    });

    Long playerId = 1L;
    MessageManager messageManager = new MessageManager();
    GatherCountdownManager gatherCountdownManager = new GatherCountdownManager();
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
            List<MessageDTO> result = messageManager.getNewMessages(getCurrentGatherId(), getSteamId(), since);
            if (result.isEmpty()) {
                return null;
            }
            return result;
        }
    }

    public NS2GServiceImpl() {
        debug = HibernateUtil.initSessionFactoryDebugRelease(forceDebug, forceRelease, "hibernate_dev.cfg.xml", "hibernate.cfg.xml");
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
                            "select pv from PlayerVote pv left join pv.votes v left join v.gather g, Gather g2 where g = g2").list();
                    for (PlayerVote playerVote : playerVotes) {
                        session.delete(playerVote);
                    }
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
    public PlayerDTO getUserName() throws LogicException, ClientAuthException {
        return getUserName(null);
    }

    public PlayerDTO getUserName(Long steamId) throws LogicException, ClientAuthException {
        if (steamId == null) {
            steamId = getSteamId();
        }
        PlayerDTO player = connectedPlayers.getNameBySteamId(steamId);
        if (player != null) {
            return player;
        }
        if (debug) {
            connectedPlayers.addPlayerBySteamId(steamId, new PlayerDTO(steamId, "fake" + steamId.toString(), System.currentTimeMillis()));
        } else {
            player = connectedPlayers.lookupPlayerBySteamId(steamId);
        }
        return player;
    }

    @Override
    public List<ServerDTO> getServers() throws LogicException, ClientAuthException {
        return HibernateUtil.queryList("from Server", new String[] {}, new Object[] {}, ServerDTO.class);
    }

    @Override
    public List<MapDTO> getMaps() throws LogicException, ClientAuthException {
        return HibernateUtil.queryList("from Map", new String[] {}, new Object[] {}, MapDTO.class);
    }

    @Override
    public void ping() throws ClientAuthException, LogicException {
        synchronized (connectedPlayers) {
            Long steamId = getSteamId();
            Long gatherId = getCurrentGatherId();
            PlayerDTO existing = connectedPlayers.getPlayerByGatherSteamId(gatherId, steamId);
            if (existing == null) {
                existing = connectedPlayers.getNameBySteamId(steamId);
                if (existing == null) {
                    return;
                }
                connectedPlayers.addPlayer(gatherId, existing);
                messageManager.postMessage(MessageType.USER_ENTERS, existing.getName(), gatherId);
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

    private void updateGatherStateByPlayerNumber(final Gather gather) throws LogicException, ClientAuthException {
        synchronized (gatherCountdownManager) {
            if (gather.getState() == GatherState.COMPLETED) {
                // nothing to do here
                return;
            }
            final Long gatherId = gather.getId();
            int connectedPlayersCount = connectedPlayers.getPlayersByGather(gatherId).size();
            if (connectedPlayersCount == Settings.GATHER_PLAYER_MAX) {
                if (gather.getState() == GatherState.OPEN) {
                    // close gather if player limit reached
                    updateGatherState(gather, GatherState.CLOSED);
                }
            } else {
                if (gather.getState() == GatherState.CLOSED) {
                    // reopen gather if players have left
                    updateGatherState(gather, GatherState.OPEN);
                }
            }
            if (connectedPlayersCount >= Settings.GATHER_PLAYER_MIN) {
                // minimum players count reached
                if (connectedPlayersCount % 2 == 0) {
                    // even number, gather may be ready
                    if (getVotedPlayersCount(gatherId) < connectedPlayersCount) {
                        // not everyone voted yet, run the timer
                        gatherCountdownManager.scheduleGatherCountdownTask(gatherId, new TimerTask() {

                            @Override
                            public void run() {
                                try {
                                    resolveGather(gather);
                                } catch (LogicException | ClientAuthException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }, Settings.GATHER_RESOLVE_DELAY);
                        updateGatherState(gather, GatherState.ONTIMER);
                        messageManager.postMessage(MessageType.RUN_TIMER, String.valueOf(Settings.GATHER_RESOLVE_DELAY / 1000), gatherId);
                    } else {
                        // gather is fully ready, let's count votes
                        countResults(gatherId);
                    }
                } else {
                    // odd number, stop the timer and wait
                    stopGatherTimer(gather);
                    messageManager.postMessage(MessageType.MORE_PLAYERS, "", gatherId);
                }
            } else {
                if (gather.getState() == GatherState.ONTIMER) {
                    stopGatherTimer(gather);
                }
            }
        }
    }

    private void stopGatherTimer(final Gather gather) throws LogicException, ClientAuthException {
        messageManager.postMessage(MessageType.STOP_TIMER, "", gather.getId());
        gatherCountdownManager.cancelGatherCountdownTasks(gather.getId());
        updateGatherState(gather, GatherState.OPEN);
    }

    protected void resolveGather(final Gather gather) throws LogicException, ClientAuthException {
        synchronized (gatherCountdownManager) {
            HibernateUtil.exec(new HibernateCallback<Void>() {

                @Override
                public Void run(Session session) throws LogicException, ClientAuthException {
                    Long gatherId = gather.getId();
                    session.enableFilter("gatherId").setParameter("gid", gatherId);
                    @SuppressWarnings("unchecked")
                    Set<Long> votedSteamIds = new HashSet<>(session.createQuery(
                            "select distinct(v.player.steamId) from Vote v left join v.gather").list());
                    GatherPlayers gatherPlayers = connectedPlayers.getPlayersByGather(gatherId);
                    Iterator<Entry<Long, PlayerDTO>> iter = gatherPlayers.entrySet().iterator();
                    while (iter.hasNext()) {
                        Entry<Long, PlayerDTO> player = iter.next();
                        if (!votedSteamIds.contains(player.getKey())) {
                            iter.remove();
                            removePlayer(gatherId, player.getKey(), true);
                        }
                    }
                    return null;
                }
            });
            updateGatherStateByPlayerNumber(gather);
        }
    }

    @Override
    public void unvote() throws LogicException, ClientAuthException {
        removeVotes(getSteamId());
        sendReadiness(getUserName().getName(), getCurrentGatherId(), false);
    }

    private void updateGatherState(Gather gather, GatherState newState) throws LogicException, ClientAuthException {
        gather.setState(newState);
        messageManager.postMessage(MessageType.GATHER_STATUS, String.valueOf(newState.ordinal()), gather.getId());
        HibernateUtil.saveObject(gather);
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
        messageManager.postMessage(MessageType.CHAT_MESSAGE, getUserName() + ": " + text, getCurrentGatherId());
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
            sendReadiness(getUserName().getName(), gatherId, true);
        }
        int connectedPlayersCount = connectedPlayers.getPlayersByGather(gatherId).size();
        if (connectedPlayersCount >= Settings.GATHER_PLAYER_MIN && getVotedPlayersCount(gatherId) == connectedPlayersCount
                && connectedPlayersCount % 2 == 0) {
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
            stopGatherTimer(gather);
            try {
                HibernateUtil.exec(new HibernateCallback<Void>() {

                    @Override
                    public Void run(Session session) throws LogicException, ClientAuthException {
                        session.enableFilter("gatherId").setParameter("gid", gatherId);
                        resetVotes(gatherId, ResetType.RESULTS);
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
                        return null;
                    }
                });
            } catch (LogicException e) {
                resetVotes(gatherId, ResetType.ALL);
                messageManager.postMessage(MessageType.VOTE_ENDED, e.getMessage(), gatherId);
                postVoteChangeMessage(gatherId);
                updateGatherStateByPlayerNumber(gather);
                return;
            }
            messageManager.postMessage(MessageType.VOTE_ENDED, "ok", gatherId);
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
        getSteamId();
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

    private void postVoteChangeMessage() throws LogicException, ClientAuthException {
        postVoteChangeMessage(getCurrentGatherId());
    }

    private void postVoteChangeMessage(Long gatherId) throws LogicException, ClientAuthException {
        messageManager.postMessage(MessageType.VOTE_CHANGE, getVoteStat(gatherId), gatherId);
    }

    @Override
    public String getVoteStat() throws LogicException, ClientAuthException {
        return getVoteStat(getCurrentGatherId());
    }

    private String getVoteStat(Long gatherId) throws LogicException, ClientAuthException {
        return String.format("%d/%d", getVotedPlayersCount(gatherId), connectedPlayers.getPlayersByGather(gatherId).size());
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

    private void validateVoteNumbers(Long[][] votes) throws LogicException, ClientAuthException {
        int idx = 0;
        for (Long[] vote : votes) {
            if (vote.length < ClientSettings.voteRules[idx].getVotesRequired()
                    || vote.length > ClientSettings.voteRules[idx].getVotesLimit()) {
                if (removeVotes(getSteamId())) {
                    sendReadiness(getUserName().getName(), getCurrentGatherId(), false);
                }
                throw LogicExceptionFormatted.format("Ожидается %s голосов за %s, получено %d. Пожалуйста, переголосуйте.",
                        ClientSettings.voteRules[idx].voteRange(), ClientSettings.voteRules[idx].getName(), vote.length);
            }
            idx++;
        }
    }

    private void sendReadiness(String username, Long gatherId, boolean ready) throws LogicException, ClientAuthException {
        postVoteChangeMessage();
        messageManager.postMessage(ready ? MessageType.USER_READY : MessageType.USER_UNREADY, username, gatherId);
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
                        "select vr from VoteResult vr left join vr.gather g, Gather g2 where vr.type = :vt and g = g2 order by vr.voteCount desc, vr.place, rand()")
                .setParameter("vt", voteType).setMaxResults(ClientSettings.voteRules[idx].getWinnerCount()).list();
        if (voteResults.size() < ClientSettings.voteRules[idx].getWinnerCount()) {
            throw LogicExceptionFormatted.format("Невозможно определить %s, переголосовка.", ClientSettings.voteRules[idx].getName());
        }
        return voteResults;
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
                @SuppressWarnings("unchecked")
                List<Remembered> remembereds = session.createQuery("from Remembered r where r.steamId = :sid").setLong("sid", getSteamId())
                        .list();
                for (Remembered remembered : remembereds) {
                    session.delete(remembered);
                }
                getSession().invalidate();
                return null;
            }
        });
    }

    @Override
    public void resetGatherPresence() throws LogicException {
        getSession().removeAttribute(Settings.GATHER_ID);
    }

    @Override
    public Set<String> getVotedPlayerNames() throws LogicException, ClientAuthException {
        return HibernateUtil.exec(new HibernateCallback<Set<String>>() {

            @Override
            public Set<String> run(Session session) throws LogicException, ClientAuthException {
                Set<String> result = new HashSet<>();
                Long gatherId = getCurrentGatherId();
                session.enableFilter("gatherId").setParameter("gid", gatherId);
                @SuppressWarnings("unchecked")
                List<Long> votedSteamIds = session.createQuery(
                        "select distinct(v.player.steamId) from Vote v left join v.gather g, Gather g2 where g = g2").list();
                GatherPlayers gatherPlayers = connectedPlayers.getPlayersByGather(gatherId);
                for (Long voteSteamId : votedSteamIds) {
                    PlayerDTO playerDTO = gatherPlayers.get(voteSteamId);
                    if (playerDTO != null) {
                        result.add(playerDTO.getName());
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
        InitStateDTO result = new InitStateDTO();
        result.setGatherState(getGatherState());
        result.setMaps(getMaps());
        result.setPlayers(getConnectedPlayers());
        result.setServers(getServers());
        result.setVotedNames(getVotedPlayerNames());
        result.setVoteStat(getVoteStat());
        result.setVersion(getVersion());
        return result;
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
        removeVotes(steamId);
        messageManager.postMessage(MessageType.USER_LEAVES, getUserName(steamId).getName(), gatherId);
        if (isKicked) {
            MessageDTO messageDTO = new MessageDTO(MessageType.USER_KICKED, "Вы были кикнуты из Gather по неактивности.", gatherId);
            messageDTO.setVisibility(MessageVisibility.PERSONAL);
            messageDTO.setToSteamId(steamId);
            messageManager.postMessage(messageDTO);
        }
        postVoteChangeMessage(gatherId);
        updateGatherStateByPlayerNumber(getGatherById(gatherId));
    }
}
