package me.rkfg.ns2gather.server;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CleanupManager {
    LinkedList<AutoCloseable> closeables = new LinkedList<>();
    Logger logger = LoggerFactory.getLogger(getClass());

    public void add(AutoCloseable cleanuppable) {
        closeables.add(cleanuppable);
    }

    public void doCleanup() throws Exception {
        for (AutoCloseable closeable : closeables) {
            logger.info("Cleaning up " + closeable.toString());
            closeable.close();
        }
    }

}
