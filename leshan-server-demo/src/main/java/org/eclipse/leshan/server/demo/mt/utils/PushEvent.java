package org.eclipse.leshan.server.demo.mt.utils;

import org.eclipse.leshan.server.demo.mt.utils.CodeWrapper.EventCode;

public interface PushEvent {
    public int getId(); 

    public EventCode getEventCode();

    public void setEventCode(EventCode ev);

    public boolean isImmediateNotify();

    public byte[] toEventListByte();

    public void addInstance(int... instances);

    public int[] getInstance();
}