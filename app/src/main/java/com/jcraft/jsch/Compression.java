package com.jcraft.jsch;
public interface Compression {
  int inflate(byte[] buf, int start, int[] len) throws Exception;
  int deflate(byte[] buf, int start, int[] len) throws Exception;
  void init(int type, int level) throws Exception;
  int INFLATER = 0;
  int DEFLATER = 1;
}
