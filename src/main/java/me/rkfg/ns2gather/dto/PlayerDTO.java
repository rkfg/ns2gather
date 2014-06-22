package me.rkfg.ns2gather.dto;

public class PlayerDTO extends CheckedDTO {
    Long lastPing;
    String profileUrl;

    public PlayerDTO(Long id, String name, String profileUrl, Long lastPing) {
        super();
        this.id = id;
        this.name = name;
        this.profileUrl = profileUrl;
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

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    @Override
    public String toString() {
        return name;
    }

}
