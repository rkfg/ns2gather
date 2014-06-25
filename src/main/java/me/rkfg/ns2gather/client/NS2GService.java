package me.rkfg.ns2gather.client;

import java.util.List;
import java.util.Set;

import me.rkfg.ns2gather.dto.GatherState;
import me.rkfg.ns2gather.dto.InitStateDTO;
import me.rkfg.ns2gather.dto.MapDTO;
import me.rkfg.ns2gather.dto.MessageDTO;
import me.rkfg.ns2gather.dto.PlayerDTO;
import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.Side;
import me.rkfg.ns2gather.dto.VoteResultDTO;
import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.LogicException;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("ns2g")
public interface NS2GService extends RemoteService {

    String login() throws LogicException;

    Long getSteamId() throws ClientAuthException, LogicException;

    PlayerDTO getUserName() throws LogicException, ClientAuthException;

    List<ServerDTO> getServers() throws LogicException, ClientAuthException;

    List<MapDTO> getMaps() throws LogicException, ClientAuthException;

    List<MessageDTO> getNewMessages(Long since);

    void ping() throws ClientAuthException, LogicException;

    void sendChatMessage(String text) throws ClientAuthException, LogicException;

    List<PlayerDTO> getConnectedPlayers() throws LogicException, ClientAuthException;

    void vote(Long[][] votes) throws LogicException, ClientAuthException;

    List<VoteResultDTO> getVoteResults() throws LogicException, ClientAuthException;

    void fakeLogin() throws ClientAuthException, LogicException;

    void logout() throws LogicException, ClientAuthException;

    void resetGatherPresence() throws LogicException;

    String getVoteStat() throws LogicException, ClientAuthException;

    Set<String> getVotedPlayerNames() throws LogicException, ClientAuthException;

    GatherState getGatherState() throws LogicException, ClientAuthException;

    InitStateDTO getInitState() throws LogicException, ClientAuthException;

    void unvote() throws LogicException, ClientAuthException;

    List<PlayerDTO> getGatherParticipantsList() throws LogicException, ClientAuthException;

    void pickSide(Side side) throws LogicException, ClientAuthException;

    void pickPlayer(Long playerSteamId) throws LogicException, ClientAuthException;
}
