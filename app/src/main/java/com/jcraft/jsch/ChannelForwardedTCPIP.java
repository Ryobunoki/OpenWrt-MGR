package com.jcraft.jsch;

/** Slim stub: remote port-forwarding not shipped. */
public class ChannelForwardedTCPIP extends Channel {
    public ChannelForwardedTCPIP() {}

    @Override
    public void run() {}

    static String[] getPortForwarding(Session session) {
        return new String[0];
    }

    static void addPort(
            Session session,
            String address_to_bind,
            int port,
            int allocated_port,
            String target,
            int target_port,
            SocketFactory factory
    ) throws JSchException {
        throw new JSchException("remote port forward not included");
    }

    static void addPort(
            Session session,
            String address_to_bind,
            int port,
            int allocated_port,
            String daemon,
            Object[] arg
    ) throws JSchException {
        throw new JSchException("remote port forward not included");
    }

    static void delPort(ChannelForwardedTCPIP c) {
        // no-op
    }

    static void delPort(Session session, int rport) {
        // no-op
    }

    static void delPort(Session session, String address, int rport) {
        // no-op
    }

    /** Invoked by Session.disconnect() on the session thread. */
    static void delPort(Session session) {
        // no-op
    }
}
