package org.mikrotik.iot.sd.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PacketConfig {
    public String mConfig;
    public int mMeasurementByteCount;
    public boolean mIsRepeatCall;
    public double mPowValue;
    public int mPow;
    public int mPacketByteSize;
    public int mMeasurementCount;
    public int mInterval;
    public int mUnixTime;
    public int mOffset;

    private static final Map<Integer, Integer> CFG_BYTES;
    static {
        Map<Integer, Integer> bytes = new HashMap<Integer, Integer>();
        bytes.put(0, 1); // 1*8=8
        bytes.put(1, 2); // 2*8=16
        bytes.put(2, 4); // 4*8=32
        bytes.put(3, 4); // 4*8=32
        CFG_BYTES = Collections.unmodifiableMap(bytes);
    }
    
    public PacketConfig(String cfg) {
        setCfg(Byte.parseByte(cfg, 2));   
    }

    public PacketConfig(byte[] cfg) {
        byte[] valueArray = ByteUtil.getEmptyByteArray(0);

        valueArray[0] = cfg[0];
        valueArray[1] = cfg[1];
        valueArray[2] = cfg[2];
        valueArray[3] = cfg[3];
        this.mUnixTime = ByteUtil.byteToInt(valueArray, false);
        // interval
        valueArray[0] = cfg[4];
        valueArray[1] = cfg[5];
        valueArray[2] = 0;
        valueArray[3] = 0;
        this.mInterval = ByteUtil.byteToInt(valueArray, false);
        // measurement count
        this.mMeasurementCount = cfg[6];
        // 1B config
        setCfg(cfg[7]);
        //!!after config decode
        // full packet size
        this.mPacketByteSize = ByteUtil.CFG_HEADER_BYTES + mMeasurementCount * mMeasurementByteCount;
        //4 byte value offset
        this.mOffset = ByteUtil.VALUE_BYTES - this.mMeasurementByteCount;
    }

    private void setCfg(byte cfg) {
        this.mConfig = ByteUtil.byteToString(cfg);
        // is repeat call
        this.mIsRepeatCall = ByteUtil.bitStringToInt(this.mConfig.substring(1, 2), false) == 1;
        // floating point set
        this.mPow = ByteUtil.bitStringToInt(this.mConfig.substring(2, 5), true); // floating point
        this.mPowValue = Math.pow(10, mPow);
        // bytes in one measurement
        this.mMeasurementByteCount = CFG_BYTES.get(ByteUtil.bitStringToInt(this.mConfig.substring(5), false)); // value bytes
    }

    public String toString() {
        return new StringBuffer().append("configByte:").append(mConfig).append("\n")
        .append("mMeasurementByteCount:").append(mMeasurementByteCount).append("\n")
        .append("mIsRepeatCall:").append(mIsRepeatCall).append("\n")
        .append("mPowValue:").append(mPowValue).append("\n")
        .append("mPow:").append(mPow).append("\n")
        .append("mPacketByteSize:").append(mPacketByteSize).append("\n")
        .append("mMeasurementCount:").append(mMeasurementCount).append("\n")
        .append("mInterval:").append(mInterval).append("\n")
        .append("mUnixTime:").append(mUnixTime).append("\n")
        .append("mOffset:").append(mOffset).append("\n")
        .toString();
    }
}