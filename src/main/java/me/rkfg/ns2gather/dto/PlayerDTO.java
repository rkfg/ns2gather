package me.rkfg.ns2gather.dto;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class PlayerDTO extends CheckedDTO {
    Long lastPing;
    String profileUrl;
    String nick;
    Long lastHiveUpdate = 0L;
    HiveStatsDTO hiveStats;
    Side side = Side.NONE;
    Long loginTimestamp = 0L;

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

    public Long getLastHiveUpdate() {
        return lastHiveUpdate;
    }

    public void setLastHiveUpdate(Long lastHiveUpdate) {
        this.lastHiveUpdate = lastHiveUpdate;
    }

    public HiveStatsDTO getHiveStats() {
        return hiveStats;
    }

    public void setHiveStats(HiveStatsDTO hiveStats) {
        this.hiveStats = hiveStats;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public Long getLoginTimestamp() {
        return loginTimestamp;
    }

    public void setLoginTimestamp(Long loginTimestamp) {
        this.loginTimestamp = loginTimestamp;
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

    public void buildLink(SafeHtmlBuilder sb, boolean skills) {
        if (getProfileUrl() != null && !getProfileUrl().isEmpty()) {
            sb.appendHtmlConstant(
                    "<a href=\"" + getProfileUrl() + "\" target=\"_blank\" title=\"" + SafeHtmlUtils.fromString(getName()).asString()
                            + "\">").appendEscaped(getEffectiveName()).appendHtmlConstant("</a>");
        } else {
            sb.appendEscaped(getEffectiveName());
        }
        if (skills) {
            addHiveStat(sb);
        }
    }

    private void addHiveStat(SafeHtmlBuilder sb) {
        if (hiveStats != null && hiveStats.getHoursPlayed() != null && hiveStats.getSkill() != null) {
            sb.appendEscaped(" [Часов:" + hiveStats.getHoursPlayed() + "] [Скилл:" + hiveStats.getSkill() + "]");
        }
    }

    public void buildInfo(SafeHtmlBuilder sb) {
        sb.appendEscaped(getEffectiveName());
        addHiveStat(sb);
    }
}
