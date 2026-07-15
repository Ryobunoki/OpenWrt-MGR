package com.jcraft.jsch;

/** Stub for classes stripped from jsch-slim.jar; must extend Request for ART verifier. */
class RequestX11 extends Request {
    RequestX11() {}

    public void request(Session session, Channel channel) throws Exception {
        // no-op: feature not shipped in slim build
    }
}
