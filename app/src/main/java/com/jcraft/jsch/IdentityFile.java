package com.jcraft.jsch;

/** Stub: public-key files not used (password auth only). */
public class IdentityFile implements Identity {
    public IdentityFile() {}

    public static IdentityFile newInstance(String name, String passphrase, JSch.InstanceLogger logger)
            throws JSchException {
        throw new JSchException("public key auth not included");
    }

    public static IdentityFile newInstance(String name, byte[] prv, byte[] pub, JSch.InstanceLogger logger)
            throws JSchException {
        throw new JSchException("public key auth not included");
    }

    @Override
    public boolean setPassphrase(byte[] passphrase) {
        return false;
    }

    @Override
    public byte[] getPublicKeyBlob() {
        return null;
    }

    @Override
    public byte[] getSignature(byte[] data) {
        return null;
    }

    @Override
    public String getAlgName() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public void clear() {}
}
