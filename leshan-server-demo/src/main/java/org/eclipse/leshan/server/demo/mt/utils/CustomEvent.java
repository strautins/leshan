package org.eclipse.leshan.server.demo.mt.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomEvent {
    
    private static final Logger LOG = LoggerFactory.getLogger(CustomEvent.class);
    public enum EventTriggerType {
        UP, DOWN, BOTH
    }

    public enum ConfEventCode {
        NO_EVENT, TEMP_EVENT, HUMID_EVENT, PRESS_EVENT, CO2_EVENT, IAQ_EVEN
    }

    private static final Map<Integer, ConfEventCode> EVENT_CODE_VALUE;
    private static final Map<ConfEventCode, Integer> EVENT_CODE;
    private static final Map<Integer, EventTriggerType> EVENT_TRIGGER_VALUE;
    private static final Map<EventTriggerType, Integer> EVENT_TRIGGER;
    static {
        Map<EventTriggerType, Integer> eventTriggerType = new HashMap<EventTriggerType, Integer>();
        eventTriggerType.put(EventTriggerType.UP, 0); 
        eventTriggerType.put(EventTriggerType.DOWN, 1); 
        eventTriggerType.put(EventTriggerType.BOTH, 2); 
        EVENT_TRIGGER = Collections.unmodifiableMap(eventTriggerType);

        Map<Integer, EventTriggerType> eventTriggerValue = new HashMap<Integer, EventTriggerType>();
        for(Entry<EventTriggerType, Integer> entry : EVENT_TRIGGER.entrySet()) {
            eventTriggerValue.put(entry.getValue(), entry.getKey());
        }
        EVENT_TRIGGER_VALUE = Collections.unmodifiableMap(eventTriggerValue);

        Map<ConfEventCode, Integer> eventCode = new HashMap<ConfEventCode, Integer>();
        eventCode.put(ConfEventCode.NO_EVENT, 0); 
        eventCode.put(ConfEventCode.TEMP_EVENT, 128); 
        eventCode.put(ConfEventCode.HUMID_EVENT, 129); 
        eventCode.put(ConfEventCode.PRESS_EVENT, 130); 
        eventCode.put(ConfEventCode.CO2_EVENT, 131); 
        eventCode.put(ConfEventCode.IAQ_EVEN, 132); 
        EVENT_CODE = Collections.unmodifiableMap(eventCode);

        Map<Integer, ConfEventCode> eventCodeValue = new HashMap<Integer, ConfEventCode>();
        for(Entry<ConfEventCode, Integer> entry : EVENT_CODE.entrySet()) {
            eventCodeValue.put(entry.getValue(), entry.getKey());
        }
        EVENT_CODE_VALUE = Collections.unmodifiableMap(eventCodeValue);
    }

    private final ConfEventCode mCode;  
    private EventTriggerType mType;
    private boolean mImmediateNotify;
    private float mValue;
    /**cfg stored as bits in int 00000101 00000000 = 0,2 instance as little_endian*/
    private int mInstances = 0;

    public CustomEvent(ConfEventCode code, EventTriggerType type, boolean immediateNotify, float value, int... instances) {
        this.mCode = code;
        this.mType = type;
        this.mImmediateNotify = immediateNotify;
        this.mValue = value;
        addInstance(instances);
    }

    public CustomEvent(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for(byte bs: b) {
            sb.append(String.format("%8s", Integer.toBinaryString(bs & 0xFF)).replace(' ', '0')); 
        }
        LOG.error("Cfg:{}", sb.toString());
        if(b.length == 8) {
            byte[] code = ByteUtil.getEmptyByteArray(3);
            code[0] = b[0];  
            String cfgStr = ByteUtil.byteToString(b[1]);
            byte[] instances = ByteUtil.getEmptyByteArray(2);
            instances[0] = b[2]; instances[1] = b[3];
            byte[] value = ByteUtil.getEmptyByteArray(0);
            value[0] = b[4]; value[1] = b[5];
            value[2] = b[6]; value[3] = b[7];

            this.mCode = EVENT_CODE_VALUE.get(ByteUtil.byteToInt(code));
            this.mType = EVENT_TRIGGER_VALUE.get(ByteUtil.bitStringToInt(cfgStr.substring(4,7), false));
            this.mImmediateNotify = ByteUtil.bitStringToInt(cfgStr.substring(7), false) == 1;
            this.mInstances = ByteUtil.byteToInt(instances, false);
            this.mValue = ByteUtil.byteArrayToFloat(value);
        } else {
            this.mCode = ConfEventCode.NO_EVENT;     
        }
    }

    public void addInstance(int... instances) {
        for(int i : instances) {
            //if (!BigInteger.valueOf(this.mInstances).testBit(i)) {
            if ((this.mInstances & (1L << i)) == 0) {
                this.mInstances += (int) Math.pow(2, i);
            }
        }    
    }

    public float getValue() {
        return this.mValue;
    }

    public EventTriggerType getEventTriggerType() {
        return this.mType;
    }

    public ConfEventCode getConfEventCode() {
        return this.mCode;
    }

    public boolean isImmediateNotify() {
        return this.mImmediateNotify;
    }

    public byte[] toByte() {
        String  evType = ByteUtil.byteToString((byte) (EVENT_TRIGGER.get(this.mType) & 0xFF));
        byte bCfg = Byte.parseByte("0000" + evType.substring(6) + (this.mImmediateNotify ? "1":"0") , 2);
        int intBits =  Float.floatToIntBits(mValue);
        byte[] result = new byte[] {
            (byte) (EVENT_CODE.get(this.mCode) & 0xFF),
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
    
    public String toString() {
        return new StringBuffer().append(this.mCode).append("/")
            .append(this.mType).append("/").append(this.mImmediateNotify)
            .append("/").append(mInstances).append("/").append(mValue).toString(); 
    }
}