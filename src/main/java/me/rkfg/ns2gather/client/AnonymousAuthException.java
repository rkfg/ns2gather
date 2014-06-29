package me.rkfg.ns2gather.client;

import ru.ppsrk.gwt.client.ClientAuthException;

public class AnonymousAuthException extends ClientAuthException {

    public AnonymousAuthException(String message) {
        super(message);
    }

    public AnonymousAuthException() {
    }

    /**
     * 
     */
    private static final long serialVersionUID = 2644114385001605815L;

}
