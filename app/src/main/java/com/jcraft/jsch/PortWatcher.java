package com.jcraft.jsch;

/**
 * Slim stub: port-forwarding not shipped. Session.disconnect() still calls delPort(Session).
 */
class PortWatcher {
    PortWatcher() {}

    static String[] getPortForwarding(Session session) {
        return new String[0];
    }

    static PortWatcher getPort(Session session, String address, int lport) throws JSchException {
        throw new JSchException("local port forward not included");
    }

    static PortWatcher addPort(
            Session session,
            String bindAddress,
            int lport,
            String host,
            int rport,
            ServerSocketFactory ssf
    ) throws JSchException {
        throw new JSchException("local port forward not included");
    }

    static void delPort(Session session, String address, int lport) throws JSchException {
        // no-op
    }

    /** Invoked by Session.disconnect() on the session thread. */
    static void delPort(Session session) {
        // no-op
    }

    public static PortWatcher addSocket(
            Session session,
            String bindAddress,
            int lport,
            String socketPath,
            ServerSocketFactory ssf
    ) throws JSchException {
        throw new JSchException("local port forward not included");
    }

    void run() {}

    void delete() {}

    void setConnectTimeout(int timeout) {}
}
