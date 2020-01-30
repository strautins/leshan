package org.eclipse.leshan.client.demo.mt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Event {

    private final int mId;
    private final String mCode;
    private final boolean mIsRegister;
    private final boolean mIsSystemEvent;
    private final Set<Integer> mInstances = new HashSet<Integer>();
    public Event(int id, String code, boolean isRegister, boolean isSystemEvent) {
        this.mId = id;
        this.mCode = code;
        this.mIsRegister = isRegister;
        this.mIsSystemEvent = isSystemEvent;
    }
    public Event(byte[] b) {
        //todo create from byte config
        this.mId = 0;
        this.mCode = "NULL";
        this.mIsRegister = false;
        this.mIsSystemEvent = false;
    }

    public int getId() {
        return mId;
    }

    public String getCode() {
        return mCode;
    }

    public void setInstance(int instance) {
        mInstances.add(instance);
    }

    public boolean isRegisterToServer() {
        return this.mIsRegister;
    }
    
    public Event getEvent() {
        return new Event(this.mId, this.mCode, this.mIsRegister, this.mIsSystemEvent);
    }

    public byte[] getByte() { 
        int count = 0;
        for(int stock : this.mInstances){
            count += Math.pow(2, stock);
         }
        byte[] result = new byte[] {
            (byte) (this.mId & 0xFF),
            (byte) 0x00,
            (byte) (count & 0xFF), //LITTLE_ENDIAN
            (byte) (count >> 8 & 0xFF)
        };
        return result;   
    }
}