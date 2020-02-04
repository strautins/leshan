package org.eclipse.leshan.client.demo.mt;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.demo.mt.utils.ByteUtil;
import org.eclipse.leshan.client.demo.mt.utils.CodeWrapper;
import org.eclipse.leshan.client.demo.mt.utils.CustomEvent;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.model.ObjectModel;

public class SensorReadings extends BaseInstanceEnabler {
    public GroupSensors mGroupSensors;
    private static final Logger LOG = LoggerFactory.getLogger(SensorReadings.class);
    public static final int R0 = 0;
    public static final int R1 = 1;
    public static final int R2 = 2;
    public static final int R3 = 3;
    public static final int R4 = 4;
    public static final int R5 = 5;
    private final Map<Integer, SensorConfig> mSensors = new HashMap<Integer, SensorConfig>();
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
        this.mSensors.put(R0, new SensorConfig(-20d, 32d, 10d, 2d, "01011000"));
        this.mSensors.put(R1, new SensorConfig(30d, 70d, 10d, 1d, "00100000"));
        this.mSensors.put(R2, new SensorConfig(990d, 1030d, 2d, 0.5d, "01011000"));
        this.mSensors.put(R3, new SensorConfig(0d, 20, 2d, 0.5d, "01011000"));
        this.mSensors.put(R4, new SensorConfig(450d, 1800d, 50d, 15d, "01000000"));
        this.mSensors.put(R5, new SensorConfig(0d, 500d, 30, 5d, "01000000"));

        for(Map.Entry<Integer, SensorConfig> entry : mSensors.entrySet() ) {
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

    public void clearEvent() {
        for(Map.Entry<Integer, SensorConfig> entry : mSensors.entrySet() ) {
            entry.getValue().clearEvent();    
        }
    }

    public SensorConfig getSensor(int resourceId) {
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
        for(Map.Entry<Integer, SensorConfig> entry : mSensors.entrySet() ) {
            entry.getValue().resetMeasurementList(d);    
        }
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        SensorConfig s = this.getSensor(resourceId);
        if(s != null) {
            // enum MeasType : uint8_t {
            //     INT8 = 0,
            //     INT16,
            //     INT32,
            //     FLOAT,
            // };
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
            Double[] itemsArray = new Double[s.getMeasurementList().size()];
            itemsArray = s.getMeasurementList().toArray(itemsArray);
            int unixTime = (int)(s.getFMT().getTime() / 1000);
            int interval = s.getInterval();
            byte[] result = new byte[] { //LITTLE_ENDIAN
                    (byte) ((unixTime) & 0xFF),
                    (byte) ((unixTime >> 8) & 0xFF),
                    (byte) ((unixTime >> 16) & 0xFF),
                    (byte) ((unixTime >> 24) & 0xFF), //uint32_t
                    (byte) (interval & 0xFF),
                    (byte) (interval >> 8 & 0xFF), //uint16_t
                    (byte) itemsArray.length,//uint8_t
                    Byte.parseByte(s.getCfg(), 2) //else uint8_t
            };
            //INT32 / 8 = 4 bytes
            //INT16 / 8 = 2 bytes
            int size = CFG_BYTES.get(ByteUtil.bitStringToInt(s.getCfg().substring(0, 3), false));
            byte[] measurementBytes = new byte[itemsArray.length * size];
            int count = 0;
            for(Double o : itemsArray) {
                int val = 0;
                if(ByteUtil.bitStringToInt(s.getCfg().substring(3, 6), true) < 0) {
                    val = (int)(GroupSensors.getDigitValue((double)o, 2) * 100);
                } else {
                    val = o.intValue();
                }
                //LOG.info("Start value:{}", val);
                for(int i = 0; i < size; i++) {
                    int shift = i * 8;
                    if(shift != 0) {
                        measurementBytes[count * size + i] = (byte) ((val >> shift) & 0xFF);
                    } else {
                        measurementBytes[count * size + i] = (byte) (val & 0xFF);
                    }
                    //LOG.info("shift:{}:{}:{}", shift, count * size + i, String.format("%8s", Integer.toBinaryString(measurementBytes[count * size + i] & 0xFF)).replace(' ', '0'));
                }
                count++;
            }
            result = ByteUtil.concatenate(result, measurementBytes);
            return ReadResponse.success(resourceId, result);
        } else {
            return super.read(identity, resourceId);
        }
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
