package me.rkfg.ns2gather.dto;

public class PlayerDTO extends CheckedDTO {
    Long lastPing;

    public PlayerDTO(Long id, String name, Long lastPing) {
        super();
        this.id = id;
        this.name = name;
        this.lastPing = lastPing;
    }

    public PlayerDTO() {
    }

    public Long getLastPing() {
        return lastPing;
    }

    public void setLastPing(Long lastPing) {
        this.lastPing = lastPing;
    }

}
