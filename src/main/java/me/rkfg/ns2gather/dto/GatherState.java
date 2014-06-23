package me.rkfg.ns2gather.dto;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum GatherState implements IsSerializable {
    OPEN, CLOSED, ONTIMER, COMPLETED
}
