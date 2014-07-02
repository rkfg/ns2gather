package me.rkfg.ns2gather.dto;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class PlayerDTO extends CheckedDTO {
    Long lastPing;
    String profileUrl;
    String nick;
    Long lastHiveUpdate = 0L;
    HiveStatsDTO hiveStats;
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
        sb.appendHtmlConstant("<a href=\"" + getProfileUrl() + "\" target=\"_blank\" title=\"" + getName() + "\">")
                .appendEscaped(getEffectiveName()).appendHtmlConstant("</a>");
        if (skills && hiveStats != null && hiveStats.getHoursPlayed() != null && hiveStats.getSkill() != null) {
            sb.appendEscaped(" [H:" + hiveStats.getHoursPlayed() + "] [S:" + hiveStats.getSkill() + "]");
        }
    }

    public String buildInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getEffectiveName());
        if (hiveStats != null && hiveStats.getHoursPlayed() != null && hiveStats.getSkill() != null) {
            sb.append(" [H:" + hiveStats.getHoursPlayed() + "] [S:" + hiveStats.getSkill() + "]");
        }
        return sb.toString();
    }
}
