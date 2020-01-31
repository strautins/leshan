package org.eclipse.leshan.client.demo.mt.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredefinedEvent {

    private static final Logger LOG = LoggerFactory.getLogger(PredefinedEvent.class);
    public enum PredefinedEventCode {
        NO_EVENT, CRIT_BAT, BATTERY_PROB, NOT_REACHABLE, DOCKED, UNDOCKED, EXT_PWR_ON,
        EXT_PWR_OFF, LOW_BT_SIGNAL, LOW_CEL_SIGNAL, ALARM
    }

    public static final Map<PredefinedEventCode, Boolean> IMMEDIATE_NOTIFY;
    public static final Map<PredefinedEventCode, Integer> PREDEFINED_EVENT;
    public static final Map<Integer, PredefinedEventCode> PREDEFINED_EVENT_VALUE;
    static {
        Map<PredefinedEventCode, Integer> ev = new HashMap<PredefinedEventCode, Integer>();
        ev.put(PredefinedEventCode.NO_EVENT, 0);
        ev.put(PredefinedEventCode.CRIT_BAT, 1);
        ev.put(PredefinedEventCode.BATTERY_PROB, 2);
        ev.put(PredefinedEventCode.NOT_REACHABLE, 3);
        ev.put(PredefinedEventCode.DOCKED, 4);
        ev.put(PredefinedEventCode.UNDOCKED, 5);
        ev.put(PredefinedEventCode.EXT_PWR_ON, 6);
        ev.put(PredefinedEventCode.EXT_PWR_OFF, 7);
        ev.put(PredefinedEventCode.LOW_BT_SIGNAL, 8);
        ev.put(PredefinedEventCode.LOW_CEL_SIGNAL, 9);
        ev.put(PredefinedEventCode.ALARM, 10);
        PREDEFINED_EVENT = Collections.unmodifiableMap(ev);

        Map<Integer, PredefinedEventCode> predefinedEvents = new HashMap<Integer, PredefinedEventCode>();
        for(Entry<PredefinedEventCode, Integer> entry : PREDEFINED_EVENT.entrySet()) {
            predefinedEvents.put(entry.getValue(), entry.getKey());
        }
        PREDEFINED_EVENT_VALUE = Collections.unmodifiableMap(predefinedEvents);

        Map<PredefinedEventCode, Boolean> imm = new HashMap<PredefinedEventCode, Boolean>();
        imm.put(PredefinedEventCode.NO_EVENT, false);
        imm.put(PredefinedEventCode.CRIT_BAT, true);
        imm.put(PredefinedEventCode.BATTERY_PROB, true);
        imm.put(PredefinedEventCode.NOT_REACHABLE, true);
        imm.put(PredefinedEventCode.DOCKED, true);
        imm.put(PredefinedEventCode.UNDOCKED, true);
        imm.put(PredefinedEventCode.EXT_PWR_ON, false);
        imm.put(PredefinedEventCode.EXT_PWR_OFF, true);
        imm.put(PredefinedEventCode.LOW_BT_SIGNAL, true);
        imm.put(PredefinedEventCode.LOW_CEL_SIGNAL, false);
        imm.put(PredefinedEventCode.ALARM, true);
        IMMEDIATE_NOTIFY = Collections.unmodifiableMap(imm);
    }
    // size = 4B
    // struct Event {
    //     uint8_t event_code;         // ConfEventCode or EventCode event codes
    //     int8_t par1;                // parameter for event based on specific code
    //     uint16_t par2;              // parameter for event based on specific code
    // }__packed; 
    private final PredefinedEventCode mCode;
    /** Reserved for ?? */
    private int mParam1 = 0;
    /**cfg stored as bits in int 00000101 00000000 = 0,2 instance as little_endian*/
    private int mParam2 = 0;

    public PredefinedEvent(PredefinedEventCode ev) {
        this.mCode = ev;
    }

    public PredefinedEvent(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for(byte bs: b) {
            sb.append(String.format("%8s", Integer.toBinaryString(bs & 0xFF)).replace(' ', '0')); 
        }
        LOG.error("Cfg:{}", sb.toString());
        if(b.length == 4) {
            byte[] code = ByteUtil.getEmptyByteArray(3);
            code[0] = b[0];  
            byte[] param1 = ByteUtil.getEmptyByteArray(3);
            param1[0] = b[1];
            byte[] param2 = ByteUtil.getEmptyByteArray(2);
            param2[0] = b[2]; param2[1] = b[3];

            this.mCode = PREDEFINED_EVENT_VALUE.get(ByteUtil.byteToInt(code));
            this.mParam1 = ByteUtil.byteToInt(param1, false);
            this.mParam2 = ByteUtil.byteToInt(param2, false);
        } else {
            this.mCode = PredefinedEventCode.NO_EVENT;   
        }
    }

    public int getId() {
        return PREDEFINED_EVENT.get(this.mCode);
    }

    public String getCode() {
        return mCode.toString();
    }

    //for event in instances
    public void addParam2(int... instances) {
        for(int i : instances) {
            //if (!BigInteger.valueOf(this.mInstances).testBit(i)) {
            if ((this.mParam2 & (1L << i)) == 0) {
                this.mParam2 += (int) Math.pow(2, i);
            }
        }    
    }

    public boolean isImmediateNotify() {
        return IMMEDIATE_NOTIFY.get(this.mCode);
    }
  
    public byte[] toByte() { 
        byte[] result = new byte[] {
            (byte) (this.getId() & 0xFF),
            (byte) 0x00,
            (byte) (this.mParam2 & 0xFF), //LITTLE_ENDIAN ???
            (byte) (this.mParam2 >> 8 & 0xFF)
        };
        return result;   
    }

    public String toString() {
        return new StringBuffer().append(this.getId()).append("/")
            .append(this.getCode()).append("/").append(this.isImmediateNotify())
            .append("/").append(this.mParam1).append("/").append(this.mParam2).toString(); 
    }
}