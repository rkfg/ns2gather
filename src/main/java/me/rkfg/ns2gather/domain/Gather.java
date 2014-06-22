package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import me.rkfg.ns2gather.dto.GatherState;
import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
@FilterDef(name = "gatherId", parameters = @ParamDef(name = "gid", type = "long"))
@Filter(name = "gatherId", condition = "id = :gid")
public class Gather extends BasicDomain {
    String name;
    GatherState state = GatherState.OPEN;
    String playersList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GatherState getState() {
        return state;
    }

    public void setState(GatherState state) {
        this.state = state;
    }

    public String getPlayersList() {
        return playersList;
    }

    public void setPlayersList(String playersList) {
        this.playersList = playersList;
    }

}
