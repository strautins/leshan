package org.eclipse.leshan.client.demo.mt.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputStateConfig extends CodeWrapper {
    
    private static final Logger LOG = LoggerFactory.getLogger(CustomEvent.class);

    private float mValue;
    private OutputTriggerType mOutputTriggerType;
    private OutputLogic mOutputLogic;
    private OutputPolarity mOutputPolarity;

    //secondary
    private EventCode m2Code;
    private float m2Value;
    private OutputTriggerType m2OutputTriggerType;

    public OutputStateConfig(OutputPolarity outputPolarity, EventCode code, float value, OutputTriggerType outputTriggerType) {
        this(outputPolarity, code, value, outputTriggerType, OutputLogic.OR, EventCode.NO_EVENT, 0, OutputTriggerType.LOWER);
    }

    public OutputStateConfig(OutputPolarity outputPolarity, EventCode code, float value, OutputTriggerType outputTriggerType, OutputLogic outputLogic,
            EventCode code2, float value2, OutputTriggerType outputTriggerType2) {
        super(code);
        this.mOutputPolarity = outputPolarity;
        this.mValue = value;
        this.mOutputTriggerType = outputTriggerType;
        this.mOutputLogic = outputLogic;

         //Second parameters
         this.m2Code = code2;
         this.m2Value = value2;
         this.m2OutputTriggerType = outputTriggerType2;
    }

    public OutputStateConfig(byte[] b) {
        super(EventCode.NO_EVENT);
        if(b.length == 11) {
            byte[] code = ByteUtil.getEmptyByteArray(3);
            code[0] = b[0];  
            super.setEventCode(ByteUtil.byteToInt(code));
            byte[] value = ByteUtil.getEmptyByteArray(0);
            value[0] = b[1]; value[1] = b[2];
            value[2] = b[3]; value[3] = b[4];
            this.mValue = ByteUtil.byteArrayToFloat(value);

            code = ByteUtil.getEmptyByteArray(3);
            code[0] = b[5];  
            this.m2Code = EVENT_CODE_VALUE.get(ByteUtil.byteToInt(code));

            value = ByteUtil.getEmptyByteArray(0);
            value[0] = b[6]; value[1] = b[7];
            value[2] = b[8]; value[3] = b[9];
            this.m2Value = ByteUtil.byteArrayToFloat(value);

            String cfgStr = ByteUtil.byteToString(b[10]);
            this.mOutputTriggerType = TYPE_BIT_LEVEL_VALUE.get(cfgStr.substring(4,5));
            this.m2OutputTriggerType = TYPE_BIT_LEVEL_VALUE.get(cfgStr.substring(5,6));
            this.mOutputLogic = LOGIC_BIT_LEVEL_VALUE.get(cfgStr.substring(6,7));
            this.mOutputPolarity = POLARITY_BIT_LEVEL_VALUE.get(cfgStr.substring(7));
        }
    }

    public byte[] toWriteByte() {
        int value1 =  Float.floatToIntBits(mValue);
        int value2 =  Float.floatToIntBits(m2Value);

        byte[] result = new byte[] {
            (byte) (super.getId() & 0xFF),
            (byte) ((value1) & 0xFF), //LITTLE_ENDIAN
            (byte) ((value1 >> 8) & 0xFF),
            (byte) ((value1 >> 16) & 0xFF),
            (byte) ((value1 >> 24) & 0xFF),
            (byte) (EVENT_CODE.get(this.m2Code) & 0xFF),
            (byte) ((value2) & 0xFF), //LITTLE_ENDIAN
            (byte) ((value2 >> 8) & 0xFF),
            (byte) ((value2 >> 16) & 0xFF),
            (byte) ((value2 >> 24) & 0xFF),
            getConfig()
        };
        return result;   
    }

    private byte getConfig() {
        return Byte.parseByte("0000" + 
        TYPE_BIT_LEVEL.get(this.mOutputTriggerType) +
        TYPE_BIT_LEVEL.get(this.m2OutputTriggerType) +
        LOGIC_BIT_LEVEL.get(this.mOutputLogic) +
        POLARITY_BIT_LEVEL.get(this.mOutputPolarity), 2);      
    }

    public float getValue() {
        return this.mValue;
    }

    public float get2Value() {
        return this.mValue;
    }

    @Override
    public String toString() {
        return new StringBuffer().append(this.getId()).append("/")
            .append(this.getEventCode().name()).append("/")
            .append(this.getEventCode().name()).append("/")
            .append(this.getEventCode().name()).append("/")
            .append(this.getEventCode().name()).append("/")
            
            
            
            
            .toString(); 
    }
}