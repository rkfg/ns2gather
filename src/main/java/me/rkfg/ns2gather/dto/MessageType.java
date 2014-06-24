package me.rkfg.ns2gather.dto;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum MessageType implements IsSerializable {
    USER_ENTERS, USER_LEAVES, CHAT_MESSAGE, USER_READY, USER_UNREADY, GAME_START, VOTE_CHANGE, VOTE_ENDED, GATHER_STATUS, USER_KICKED, RUN_TIMER, STOP_TIMER, MORE_PLAYERS, RESET_HIGHLIGHT, SIDE_PICKED, MARINE_PICKED, ALIEN_PICKED
}
