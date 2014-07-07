package me.rkfg.ns2gather.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.SteamPlayerDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.ppsrk.gwt.client.ClientAuthException;
import ru.ppsrk.gwt.client.LogicException;
import ru.ppsrk.gwt.server.HibernateUtil;
import ru.ppsrk.gwt.server.ServerUtils;

import com.github.koraktor.steamcondenser.steam.SteamPlayer;
import com.github.koraktor.steamcondenser.steam.servers.SourceServer;

public class ServerManager implements AutoCloseable {

    Logger logger = LoggerFactory.getLogger(getClass());

    public class ServerData {
        HashMap<String, SteamPlayer> players = new HashMap<>();

        public int getPlayersCount() {
            return players.size();
        }

        public Collection<SteamPlayer> getSteamPlayers() {
            return players.values();
        }

        public Set<String> getSteamPlayersNames() {
            return players.keySet();
        }

        public void setPlayers(HashMap<String, SteamPlayer> players) {
            this.players = players;
        }
    }

    public interface ServersChangeCallback {
        public void onChange();
    }

    private ServersChangeCallback serversChangeCallback;

    HashMap<Long, ServerData> serversData = new HashMap<>();
    Timer serverRefresher = new Timer("Server refresher", true);

    public ServerManager(ServersChangeCallback callback) {
        serversChangeCallback = callback;
        runServersInfoRefresher();
    }

    private void fillPlayers(List<ServerDTO> servers) {
        for (ServerDTO serverDTO : servers) {
            ServerData serverData = serversData.get(serverDTO.getId());
            if (serverData != null) {
                serverDTO.setPlayers(ServerUtils.mapArray(serverData.getSteamPlayers(), SteamPlayerDTO.class));
            }
        }
    }

    protected List<ServerDTO> getRawServers() throws LogicException, ClientAuthException {
        return HibernateUtil.queryList("from Server", new String[] {}, new Object[] {}, ServerDTO.class);
    }

    protected ServerData getServerData(Long id) {
        ServerData result = serversData.get(id);
        if (result == null) {
            result = new ServerData();
            serversData.put(id, result);
        }
        return result;
    }

    public List<ServerDTO> getServers() throws LogicException, ClientAuthException {
        List<ServerDTO> servers = getRawServers();
        fillPlayers(servers);
        return servers;
    }

    private void runServersInfoRefresher() {
        serverRefresher.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    for (ServerDTO serverDTO : getRawServers()) {
                        try {
                            SourceServer sourceServer = new SourceServer(serverDTO.getIp());
                            getServerData(serverDTO.getId()).setPlayers(sourceServer.getPlayers());
                        } catch (Throwable e) {
                            logger.warn("Can't retrieve server {} [{}] players: {}", serverDTO.getName(), serverDTO.getIp(), e);
                        }
                    }
                } catch (LogicException | ClientAuthException e) {
                    e.printStackTrace();
                }
                serversChangeCallback.onChange();
            }
        }, 1000, Settings.SERVER_INFO_REFRESH_PERIOD);
    }

    @Override
    public void close() throws Exception {
        serverRefresher.cancel();
    }
}
