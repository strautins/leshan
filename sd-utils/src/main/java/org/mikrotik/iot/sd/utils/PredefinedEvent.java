package org.mikrotik.iot.sd.utils;

import java.util.Arrays;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class PredefinedEvent extends CodeWrapper implements PushEvent {

    //private static final Logger LOG = LoggerFactory.getLogger(PredefinedEvent.class);
    // size = 4B
    // struct Event {
    //     uint8_t event_code;         // ConfEventCode or EventCode event codes
    //     int8_t par1;                // parameter for event based on specific code
    //     uint16_t par2;              // parameter for event based on specific code
    // }__packed; 
    /** Reserved for ?? */
    private int mParam1 = 0;
    /**cfg stored as bits in int 00000101 00000000 = 0,2 instance as little_endian*/
    private int mParam2 = 0;

    public PredefinedEvent(EventCode ev) {
        super(ev);
    }

    public PredefinedEvent(byte[] b) {
        super(EventCode.NO_EVENT);
        if(b.length == 4) {
            byte[] code = ByteUtil.getEmptyByteArray(3);
            code[0] = b[0];  
            byte[] param1 = ByteUtil.getEmptyByteArray(3);
            param1[0] = b[1];
            byte[] param2 = ByteUtil.getEmptyByteArray(2);
            param2[0] = b[2]; param2[1] = b[3];
            super.setEventCode(ByteUtil.byteToInt(code));
            this.mParam1 = ByteUtil.byteToInt(param1);
            this.mParam2 = ByteUtil.byteToInt(param2);
        }
    }

    //for event in instances
    public void addParam2(int... instances) {
        for(int i : instances) {
            if ((this.mParam2 & (1L << i)) == 0) {
                this.mParam2 += (int) Math.pow(2, i);
            }
        }    
    }

    @Override
    public byte[] toEventListByte() { 
        byte[] result = new byte[] {
            (byte) (this.getId() & 0xFF),
            (byte) 0x00,
            (byte) (this.mParam2 & 0xFF), //LITTLE_ENDIAN ???
            (byte) (this.mParam2 >> 8 & 0xFF)
        };
        return result;   
    }
    
    @Override
    public int[] getInstance() {
        return super.getInstance(this.mParam2);
    }

    @Override
    public void addInstance(int... instances) {
        for(int i : instances) {
            if (!ByteUtil.bitLevel(this.mParam2, i)) {
                this.mParam2 += (int) Math.pow(2, i);
            }
        }    
    }

    @Override
    public String toString() {
        return new StringBuffer().append(this.getId()).append("/")
            .append(this.getEventCode().name()).append("/")
            .append(this.isImmediateNotify()).append("/")
            .append(this.mParam1).append("/")
            .append(Arrays.toString(this.getInstance()))
            .toString(); 
    }
}