package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import me.rkfg.ns2gather.dto.CheckedDTO;
import me.rkfg.ns2gather.dto.MapDTO;
import me.rkfg.ns2gather.dto.ServerDTO;
import me.rkfg.ns2gather.dto.VoteResultDTO;
import me.rkfg.ns2gather.dto.VoteType;
import me.rkfg.ns2gather.server.GatherPlayersManager.GatherPlayers;

import org.hibernate.Session;

import ru.ppsrk.gwt.client.LogicException;
import ru.ppsrk.gwt.domain.BasicDomain;
import ru.ppsrk.gwt.server.HibernateUtil;
import ru.ppsrk.gwt.server.LogicExceptionFormatted;
import ru.ppsrk.gwt.server.ServerUtils;

@Entity
public class VoteResult extends BasicDomain {
    @ManyToOne
    Gather gather;
    VoteType type;
    Long targetId;
    Long voteCount;
    Long place;

    public VoteResult(Gather gather, VoteType type, Long targetId, Long voteCount) {
        super();
        this.gather = gather;
        this.type = type;
        this.targetId = targetId;
        this.voteCount = voteCount;
    }

    public VoteResult() {
    }

    public Gather getGather() {
        return gather;
    }

    public void setGather(Gather gather) {
        this.gather = gather;
    }

    public VoteType getType() {
        return type;
    }

    public void setType(VoteType type) {
        this.type = type;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Long voteCount) {
        this.voteCount = voteCount;
    }

    public Long getPlace() {
        return place;
    }

    public void setPlace(Long place) {
        this.place = place;
    }

    public void inc() {
        setVoteCount(getVoteCount() + 1);
    }

    public VoteResultDTO toDTO(Session session, GatherPlayers gatherPlayers) throws LogicException {
        CheckedDTO target = getTarget(session, gatherPlayers, getType(), getTargetId());
        return new VoteResultDTO(getTargetId(), getVoteCount(), getType(), target);
    }

    public static CheckedDTO getTarget(Session session, GatherPlayers gatherPlayers, VoteType type, Long targetId) throws LogicException {
        CheckedDTO target = null;
        switch (type) {
        case COMM:
            target = gatherPlayers.getParticipant(targetId);
            if (target == null) {
                target = gatherPlayers.getPlayer(targetId);
                if (target == null) {
                    throw LogicExceptionFormatted.format("Игрок %d не найден.", targetId);
                }
            }
            break;
        case MAP:
            Map map = HibernateUtil.tryGetObject(targetId, Map.class, session, "Карта не найдена.");
            target = ServerUtils.mapModel(map, MapDTO.class);
            break;
        case SERVER:
            Server server = HibernateUtil.tryGetObject(targetId, Server.class, session, "Сервер не найден.");
            target = ServerUtils.mapModel(server, ServerDTO.class);
            break;
        }
        return target;
    }
}
