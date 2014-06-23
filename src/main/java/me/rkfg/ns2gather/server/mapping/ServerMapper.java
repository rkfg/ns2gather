package me.rkfg.ns2gather.server.mapping;

import me.rkfg.ns2gather.domain.Server;
import me.rkfg.ns2gather.dto.ServerDTO;

import org.dozer.DozerConverter;

public class ServerMapper extends DozerConverter<Server, ServerDTO> {

    public ServerMapper() {
        super(Server.class, ServerDTO.class);
    }

    @Override
    public ServerDTO convertTo(Server source, ServerDTO destination) {
        ServerDTO result = new ServerDTO(source.getId(), source.getName(), source.getIp() + ":" + source.getPort(), source.getPassword());
        return result;
    }

    @Override
    public Server convertFrom(ServerDTO source, Server destination) {
        return null;
    }

}
