package org.eclipse.leshan.server.demo.mt.tb;

import java.util.ArrayList;

public interface ThingsboardSend {
    public void send(String token, ArrayList<String> msg);

    public void start();
    
    public void stop();
  }