package com.github.sgargan.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SyslogTcpStream extends OutputStream {

  private InetAddress address;
  private Socket socket;
  private OutputStream out;
  final private int port;

  public SyslogTcpStream(String syslogHost, int port) throws UnknownHostException, SocketException, IOException {
    this.address = InetAddress.getByName(syslogHost);
    this.port = port;
    this.socket = new Socket(address, port);
    this.out = socket.getOutputStream();
  }

  public void write(byte[] byteArray, int offset, int len) throws IOException {
    out.write(byteArray, offset, len);
  }

  public void flush() throws IOException {
    out.flush();
  }

  public void close() {
    try {
      out.close();
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public int getPort() {
    return port;
  }

  @Override
  public void write(int b) throws IOException {
    out.write(b);
  }

}
