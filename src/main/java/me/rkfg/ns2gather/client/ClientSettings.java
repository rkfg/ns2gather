package me.rkfg.ns2gather.client;

import me.rkfg.ns2gather.dto.RuleDTO;

public class ClientSettings {

    static final int PING_INTERVAL = 3000;
    public static RuleDTO[] voteRules = { new RuleDTO(1, 1, 2, "командира"), new RuleDTO(1, 2, 1, "карту"), new RuleDTO(1, 1, 1, "сервер") };

}
