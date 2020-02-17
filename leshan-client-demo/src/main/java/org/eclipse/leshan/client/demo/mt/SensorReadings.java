package org.eclipse.leshan.client.demo.mt;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.mikrotik.iot.sd.utils.ByteUtil;
import org.mikrotik.iot.sd.utils.CodeWrapper;
import org.mikrotik.iot.sd.utils.CustomEvent;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.model.ObjectModel;

public class SensorReadings extends BaseInstanceEnabler {
    private GroupSensors mGroupSensors;
    public static final int R0 = 0;
    public static final int R1 = 1;
    public static final int R2 = 2;
    public static final int R3 = 3;
    public static final int R4 = 4;
    public static final int R5 = 5;
    private final Map<Integer, SensorEngine> mSensors = new HashMap<Integer, SensorEngine>();
    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2, R3, R4);

    // enum MeasType : uint8_t {
    //     INT8 = 0,
    //     INT16,
    //     INT32,
    //     FLOAT,
    // };
    private static final Map<Integer, Integer> CFG_BYTES;
    static {
        Map<Integer, Integer> bytes = new HashMap<Integer, Integer>();
        //from server to client params
        bytes.put(0, 1); //1*8=8
        bytes.put(1, 2); //2*8=16
        bytes.put(2, 4); //4*8=32
        bytes.put(3, 4); //4*8=32 
        CFG_BYTES = Collections.unmodifiableMap(bytes);
    }

    public SensorReadings() {
        this.mSensors.put(R0, new SensorEngine(60, -20d, 32d, 2d, 0.5d, "01011000"));
        this.mSensors.put(R1, new SensorEngine(60, 30d, 70d, 10d, 1d, "00100000"));
        this.mSensors.put(R2, new SensorEngine(60, 990d, 1030d, 2d, 0.5d, "01011000"));
        this.mSensors.put(R3, new SensorEngine(60, 450d, 1800d, 50d, 15d, "01000000"));
        this.mSensors.put(R4, new SensorEngine(60, 0d, 20, 2d, 0.5d, "01011000"));
        this.mSensors.put(R5, new SensorEngine(60, 0d, 500d, 30, 5d, "01000000"));

        for(Map.Entry<Integer, SensorEngine> entry : mSensors.entrySet() ) {
            entry.getValue().setSensorReadings(this);  
        }
    }

    public void setEvent(CustomEvent customEvent) {
        if(customEvent.getEventCode().name().equals(CodeWrapper.EventCode.TEMP_EVENT.name())) {
            this.getSensor(R0).setEvent(customEvent);
        } else if(customEvent.getEventCode().name().equals(CodeWrapper.EventCode.HUMID_EVENT.name())) {
            this.getSensor(R1).setEvent(customEvent);
        } else if(customEvent.getEventCode().name().equals(CodeWrapper.EventCode.PRESS_EVENT.name())) {
            this.getSensor(R2).setEvent(customEvent);
        } else if(customEvent.getEventCode().name().equals(CodeWrapper.EventCode.CO2_EVENT.name())) {
            this.getSensor(R3).setEvent(customEvent);
        } else if(customEvent.getEventCode().name().equals(CodeWrapper.EventCode.IAQ_EVEN.name())) {
            this.getSensor(R5).setEvent(customEvent);
        } 
    }

    public void clearTarget() {
        for(Map.Entry<Integer, SensorEngine> entry : mSensors.entrySet() ) {
            entry.getValue().reset();
        }
    }

    public void clearEvent() {
        for(Map.Entry<Integer, SensorEngine> entry : mSensors.entrySet() ) {
            entry.getValue().clearEvent();    
        }
    }

    public SensorEngine getSensor(int resourceId) {
        return this.mSensors.get(resourceId);
    }

    public void setGroupSensors(GroupSensors groupSensors) {
        this.mGroupSensors = groupSensors;
    }

    public GroupSensors getGroupSensors() {
        return this.mGroupSensors;
    }

    public void clearData(String parameters) {
        Date d = new Date(); //todo add correct clear date
        for(Map.Entry<Integer, SensorEngine> entry : mSensors.entrySet() ) {
            entry.getValue().resetMeasurementList(d);    
        }
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        SensorEngine s = this.getSensor(resourceId);
        if(s != null) {
            int valuesInPacket = 5; //set max values in packet(testing server decode)
            int packetValueCount = Math.min(valuesInPacket, s.getMeasurementList().size());
            int packetCount = packetValueCount == 0 ? 1 : (int)ByteUtil.getDoubleRoundUp((double)s.getMeasurementList().size() / (double)packetValueCount, 0);
            byte[] result = new byte[0]; 
            int pow = ByteUtil.bitStringToInt(s.getCfg().substring(3, 6), true);
            //-3 to 3;1 to -1;...
            pow = -pow; //reverse sign for encode 
            double powValue = Math.pow(10, pow);
            for (int i = 0; i < packetCount; i++) {
                byte[] valueArray;
                //left values or packetValueCount
                int localPacketValueCount = Math.min(s.getMeasurementList().size() - i * packetValueCount, packetValueCount);
                if(localPacketValueCount != 0) {
                    int valueByteSize = CFG_BYTES.get(ByteUtil.bitStringToInt(s.getCfg().substring(0, 3), false));
                    valueArray = new byte[localPacketValueCount * valueByteSize];
                    for (int j = 0; j < localPacketValueCount; j++) {
                        //(0*5 = 0) + j(0,1,2...); (1*5 = 5) + j(0,1,2...);...
                        Double o = s.getMeasurementList().get(i * localPacketValueCount + j);
                        //encode 3.21321 and pow = -3/reverse = 3; powValue = 10^3 = 1000; round (3)3.213 * powValue; result 3213
                        //decode 3213 * (10 ^ -3 = 0.001) = 3.213;
                        //encode 4321 and pow = 1/reverse = -1; powValue 10^-1= 0.1; 4321 round (-1)4320 * powValue; result 432.0
                        //decode = 432 * (10 ^ 1 = 10) = 4320
                        int val = (int)(ByteUtil.getDoubleRound((double)o, pow) * powValue);
                        //fill bytes
                        for(int k = 0; k < valueByteSize; k++) {
                            int shift = k * 8;
                            if(shift != 0) {
                                valueArray[j * valueByteSize + k] = (byte) ((val >> shift) & 0xFF);
                            } else {
                                valueArray[j * valueByteSize + k] = (byte) (val & 0xFF);
                            }
                        }
                    }
                } else {
                    valueArray = new byte[0];   
                }
                result = ByteUtil.concatenate(result, getHeader(s, i, localPacketValueCount), valueArray);    
            }
            return ReadResponse.success(resourceId, result);
        } else {
            return super.read(identity, resourceId);
        }
    }
    private byte[] getHeader(SensorEngine s, int packet, int packetCount) {
            // Structure size = 8B
            // struct MeasResHeader {
            //     uint32_t time;              // first measurement time
            //     uint16_t interval;          // measurement interval
            //     uint8_t meas_count;         // measurement count in this block
            //     uint8_t type : 3;           // measurement type (MeasType)
            //     uint8_t multi : 3;          // indicate multiplier of measurement value as value*10^multi
            //     uint8_t have_more_data : 1; // have more buffered measurements
            //     uint8_t reserved : 1;       // not used
            // }__packed;
            //1 * 5 * 60s = 300sec to next packet
            int interval = s.getInterval();
            int unixTime = (int)(s.getFMT().getTime() / 1000) + (packet * packetCount * s.getInterval());
            return new byte[] { //LITTLE_ENDIAN
                (byte) ((unixTime) & 0xFF),
                (byte) ((unixTime >> 8) & 0xFF),
                (byte) ((unixTime >> 16) & 0xFF),
                (byte) ((unixTime >> 24) & 0xFF),
                (byte) (interval & 0xFF),
                (byte) (interval >> 8 & 0xFF),
                (byte) packetCount,
                Byte.parseByte(s.getCfg(), 2)
            };
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        return super.execute(identity, resourceId, params);
    }

    @Override
    public synchronized WriteResponse write(ServerIdentity identity, int resourceId, LwM2mResource value) {
        return super.write(identity, resourceId, value);
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
