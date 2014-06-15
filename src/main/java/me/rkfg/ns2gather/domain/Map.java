package me.rkfg.ns2gather.domain;

import javax.persistence.Entity;

import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
public class Map extends BasicDomain {
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
