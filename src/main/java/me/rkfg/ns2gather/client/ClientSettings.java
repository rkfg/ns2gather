package me.rkfg.ns2gather.client;

import me.rkfg.ns2gather.dto.RuleDTO;

public class ClientSettings {

    static final int PING_INTERVAL = 3000;
    static final int SIZE_SAVE_INTERVAL = 10000;
    public static final int CHAT_MAX_LENGTH = 256;
    public static RuleDTO[] voteRules = { new RuleDTO(1, 1, 2, "командира"), new RuleDTO(1, 2, 1, "карту"), new RuleDTO(1, 1, 1, "сервер") };

}
