package com.jcraft.jsch;

/** Slim stub: X11 forwarding not shipped. */
class ChannelX11 extends Channel {
    ChannelX11() {}

    @Override
    public void run() {}

    static void setCookie(String cookie) {}

    static void setHost(String host) {}

    static void setPort(int port) {}

    static byte[] getFakedCookie(Session session) {
        return new byte[16];
    }

    /** Invoked by Session.disconnect() on the session thread. */
    static void removeFakedCookie(Session session) {
        // no-op
    }
}
