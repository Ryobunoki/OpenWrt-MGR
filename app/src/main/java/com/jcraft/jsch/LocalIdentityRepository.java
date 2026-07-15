package com.jcraft.jsch;

import java.util.Vector;

/**
 * Minimal stand-in for the class stripped from jsch-slim.jar.
 * Real JSch.<init> calls LocalIdentityRepository(InstanceLogger).
 */
public class LocalIdentityRepository implements IdentityRepository {
    private final Vector identities = new Vector();
    @SuppressWarnings("unused")
    private final JSch.InstanceLogger instLogger;

    public LocalIdentityRepository() {
        this.instLogger = null;
    }

    LocalIdentityRepository(JSch.InstanceLogger instLogger) {
        this.instLogger = instLogger;
    }

    @Override
    public String getName() {
        return "Local Identity Repository";
    }

    @Override
    public int getStatus() {
        return RUNNING;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Vector getIdentities() {
        return identities;
    }

    @Override
    public boolean add(byte[] identity) {
        return false;
    }

    @Override
    public boolean remove(byte[] blob) {
        return false;
    }

    @Override
    public void removeAll() {
        identities.removeAllElements();
    }

    public void add(Identity identity) {
        if (identity != null) {
            identities.addElement(identity);
        }
    }

    public void remove(Identity identity) {
        if (identity != null) {
            identities.removeElement(identity);
        }
    }
}
