package org.mikrotik.iot.sd.utils;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomEvent extends CodeWrapper implements PushEvent {
    
    private static final Logger LOG = LoggerFactory.getLogger(CustomEvent.class);

    private EventTriggerType mType;
    private boolean mImmediateNotify;
    private float mValue;
    /**cfg stored as bits in int 00000101 00000000 = 0,2 instance as little_endian*/
    private int mInstances = 0;

    public CustomEvent(EventCode code, EventTriggerType type, boolean immediateNotify, float value, int... instances) {
        super(code);
        this.mType = type;
        this.mImmediateNotify = immediateNotify;
        this.mValue = value;
        addInstance(instances);
    }

    public CustomEvent(byte[] b) {
        super(EventCode.NO_EVENT);
        if(b.length == 8) {
            byte[] code = ByteUtil.getEmptyByteArray(3);
            code[0] = b[0];  
            String cfgStr = ByteUtil.byteToString(b[1]);
            byte[] instances = ByteUtil.getEmptyByteArray(2);
            instances[0] = b[2]; instances[1] = b[3];
            byte[] value = ByteUtil.getEmptyByteArray(0);
            value[0] = b[4]; value[1] = b[5];
            value[2] = b[6]; value[3] = b[7];
            super.setEventCode(ByteUtil.byteToInt(code));
            this.mType = EVENT_TRIGGER_VALUE.get(ByteUtil.bitStringToInt(cfgStr.substring(4,7), false));
            this.mImmediateNotify = ByteUtil.bitStringToInt(cfgStr.substring(7), false) == 1;
            this.mInstances = ByteUtil.byteToInt(instances, false);
            this.mValue = ByteUtil.byteArrayToFloat(value);
        }
        LOG.warn("CustomEvent:{}:{}:{}:{}:{}:{}",b.length, super.getEventCode(),  this.mType, this.mImmediateNotify, this.mInstances, this.mValue);
    }

    public float getValue() {
        return this.mValue;
    }

    public EventTriggerType getEventTriggerType() {
        return this.mType;
    }

    public byte[] toWriteByte() {
        String  evType = ByteUtil.byteToString((byte) (EVENT_TRIGGER.get(this.mType) & 0xFF));
        byte bCfg = Byte.parseByte("0000" + evType.substring(6) + (this.mImmediateNotify ? "1":"0") , 2);
        int intBits =  Float.floatToIntBits(this.mValue);
        byte[] result = new byte[] {
            (byte) (super.getId() & 0xFF),
            (byte) bCfg,
            (byte) (this.mInstances & 0xFF), //LITTLE_ENDIAN ???
            (byte) (this.mInstances >> 8 & 0xFF),
            (byte) ((intBits) & 0xFF), //LITTLE_ENDIAN
            (byte) ((intBits >> 8) & 0xFF),
            (byte) ((intBits >> 16) & 0xFF),
            (byte) ((intBits >> 24) & 0xFF)
        };
        return result;   
    }

    @Override
    public boolean isImmediateNotify() {
        return this.mImmediateNotify;
    }

    //add in event list bytes
    @Override
    public byte[] toEventListByte() { 
        byte[] result = new byte[] {
            (byte) (super.getId() & 0xFF),
            (byte) 0x00,
            (byte) (this.mInstances & 0xFF), //LITTLE_ENDIAN ???
            (byte) (this.mInstances >> 8 & 0xFF),
        };
        return result;   
    }

    @Override
    public int[] getInstance() {//todo: improve performance
        return super.getInstance(this.mInstances);
    }

    @Override
    public void addInstance(int... instances) {
        for(int i : instances) {
            if (!ByteUtil.bitLevel(this.mInstances, i)) {
                this.mInstances += (int) Math.pow(2, i);
            }
        }    
    }

    @Override
    public boolean isValid() {
        return super.getEventCode() != null 
            && this.mType != null;
    }

    @Override
    public String toString() {
        return new StringBuffer().append(super.getEventCode().name()).append("/")
            .append(this.getEventTriggerType().name()).append("/").append(this.isImmediateNotify())
            .append("/").append(Arrays.toString(this.getInstance())).append("/").append(mValue).toString(); 
    }
}