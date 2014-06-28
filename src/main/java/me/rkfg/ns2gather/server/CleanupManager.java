package me.rkfg.ns2gather.server;

import java.util.LinkedList;

public final class CleanupManager {
    private static CleanupManager instance;
    LinkedList<Cleanupable> cleanuppables = new LinkedList<>();

    private CleanupManager() {
    }

    public static CleanupManager getInstance() {
        if (instance == null) {
            instance = new CleanupManager();
        }
        return instance;
    }

    public void add(Cleanupable cleanuppable) {
        cleanuppables.add(cleanuppable);
    }

    public void doCleanup() {
        for (Cleanupable cleanuppable : cleanuppables) {
            cleanuppable.cleanup();
        }
    }

}
