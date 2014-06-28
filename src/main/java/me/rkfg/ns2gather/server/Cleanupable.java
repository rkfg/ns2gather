package me.rkfg.ns2gather.server;

public abstract class Cleanupable {

    abstract void cleanup();

    public Cleanupable() {
        CleanupManager.getInstance().add(this);
    }

}
