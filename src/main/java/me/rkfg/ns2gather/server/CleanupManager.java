package me.rkfg.ns2gather.server;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CleanupManager {
    HashSet<AutoCloseable> closeables = new HashSet<>();
    Logger logger = LoggerFactory.getLogger(getClass());

    public synchronized void add(AutoCloseable closeable) {
        closeables.add(closeable);
    }

    public synchronized void remove(AutoCloseable closeable) {
        closeables.remove(closeable);
    }

    public synchronized void doCleanup() throws Exception {
        for (AutoCloseable closeable : closeables) {
            logger.info("Cleaning up " + closeable.toString());
            closeable.close();
        }
    }

}
