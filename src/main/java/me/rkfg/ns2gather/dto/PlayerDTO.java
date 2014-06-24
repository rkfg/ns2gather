package me.rkfg.ns2gather.dto;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class PlayerDTO extends CheckedDTO {
    Long lastPing;
    String profileUrl;
    Side side = Side.NONE;

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

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    @Override
    public String toString() {
        return name;
    }

    public static AbstractCell<PlayerDTO> getCell() {
        return new AbstractCell<PlayerDTO>() {
            @Override
            public void render(Context context, PlayerDTO value, SafeHtmlBuilder sb) {
                sb.appendEscaped(value.getName());
            }
        };
    }
}
