package com.jcraft.jsch;

import java.util.Vector;

/** Stub for identity-repo wrapper stripped from jsch-slim.jar. */
public class IdentityRepositoryWrapper implements IdentityRepository {
    private final IdentityRepository repo;

    public IdentityRepositoryWrapper() {
        this.repo = null;
    }

    public IdentityRepositoryWrapper(IdentityRepository repo) {
        this.repo = repo;
    }

    public IdentityRepositoryWrapper(IdentityRepository repo, boolean keep) {
        this.repo = repo;
    }

    @Override
    public String getName() {
        return repo != null ? repo.getName() : "wrapper";
    }

    @Override
    public int getStatus() {
        return repo != null ? repo.getStatus() : RUNNING;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Vector getIdentities() {
        return repo != null ? repo.getIdentities() : new Vector();
    }

    @Override
    public boolean add(byte[] identity) {
        return repo != null && repo.add(identity);
    }

    @Override
    public boolean remove(byte[] blob) {
        return repo != null && repo.remove(blob);
    }

    @Override
    public void removeAll() {
        if (repo != null) repo.removeAll();
    }

    public void add(Identity identity) {
        // no-op for password-only shell use
    }
}
