package me.rkfg.ns2gather.dto;

import com.google.gwt.user.client.rpc.IsSerializable;

public class MessageDTO implements IsSerializable {
    MessageVisibility visibility;
    Long toSteamId;
    MessageType type;
    String content;
    Long timestamp;
    Long gatherId;

    public static MessageDTO privateMessage(Long toSteamId, MessageType type, String content, Long gatherId) {
        MessageDTO result = new MessageDTO();
        result.visibility = MessageVisibility.PERSONAL;
        result.toSteamId = toSteamId;
        result.type = type;
        result.content = content;
        result.gatherId = gatherId;
        return result;
    }

    public MessageDTO(MessageType type, String content, Long gatherId) {
        super();
        this.type = type;
        this.content = content;
        this.gatherId = gatherId;
        visibility = MessageVisibility.BROADCAST;
        timestamp = System.currentTimeMillis();
    }

    public MessageDTO() {
    }

    public MessageVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(MessageVisibility visibility) {
        this.visibility = visibility;
    }

    public Long getToSteamId() {
        return toSteamId;
    }

    public void setToSteamId(Long toSteamId) {
        this.toSteamId = toSteamId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getGatherId() {
        return gatherId;
    }

    public void setGatherId(Long gatherId) {
        this.gatherId = gatherId;
    }

}
