package com.jcraft.jsch;
public class UserAuthPublicKey extends UserAuth {
  public boolean start(Session session) throws Exception { return false; }
}
