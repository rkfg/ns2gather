package me.rkfg.ns2gather.dto;

import ru.ppsrk.gwt.client.HasId;

public class CheckedDTO implements HasId {
    Long id;
    Boolean checked = false;
    String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
