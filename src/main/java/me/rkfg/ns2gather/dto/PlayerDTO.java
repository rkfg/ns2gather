package me.rkfg.ns2gather.dto;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class PlayerDTO extends CheckedDTO {
    Long lastPing;
    String profileUrl;
    String nick;
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

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
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

    public PlayerDTO clone() {
        PlayerDTO result = new PlayerDTO(id, name, profileUrl, lastPing);
        result.setNick(getNick());
        return result;
    }

    public String getEffectiveName() {
        if (nick != null && !nick.isEmpty()) {
            return nick;
        }
        return name;
    }

    public void buildLink(SafeHtmlBuilder sb) {
        sb.appendHtmlConstant("<a href=\"" + getProfileUrl() + "\" target=\"_blank\" title=\"" + getName() + "\">")
                .appendEscaped(getEffectiveName()).appendHtmlConstant("</a>");
    }

}
