package org.eclipse.leshan.client.demo.mt;

import java.lang.reflect.Array;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.model.ObjectModel;

public class SensorReadings extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(SensorReadings.class);
    public static final int R0 = 0;
    public static final int R1 = 1;
    public static final int R2 = 2;
    public static final int R3 = 3;
    public static final int R4 = 4;
    private final SensorConfig mTemperature;
    private final SensorConfig mHumidity;
    private final SensorConfig mAtmospheric;
    private final SensorConfig mCO;
    private final SensorConfig mCO2;
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
        this.mTemperature = new SensorConfig(-20d, 32d, 2d, 0.5d, "01011000"); 
        this.mHumidity = new SensorConfig(30d, 70d, 10d, 1d, "00100000"); 
        this.mAtmospheric = new SensorConfig(990d, 1030d, 2d, 0.5d, "01011000"); 
        this.mCO = new SensorConfig(0d, 20, 2d, 0.5d, "01011000"); 
        this.mCO2 = new SensorConfig(450d, 1800d, 100d, 15d, "01000000"); 
    }
    public void clearData(String parameters) {
        Date d = new Date();
        this.mTemperature.resetMeasurementList(d);
        this.mHumidity.resetMeasurementList(d);
        this.mAtmospheric.resetMeasurementList(d);
        this.mCO.resetMeasurementList(d);
        this.mCO2.resetMeasurementList(d);
    }
    public SensorConfig getSensor(int resource) {
        SensorConfig result = null;
        switch (resource) {
            case R0:  result =  this.mTemperature;
                     break;
            case R1:  result =  this.mHumidity;
                     break;
            case R2:  result =  this.mAtmospheric;
                     break;
            case R3:  result =  this.mCO2;
                     break;
            case R4:  result =  this.mCO;
                     break;
        }
        return result;
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        SensorConfig s = getSensor(resourceId);
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
            int size = CFG_BYTES.get(bitStringToInt(s.getCfg().substring(0, 3), false));
            byte[] measurementBytes = new byte[itemsArray.length * size];
            int count = 0;
            for(Double o : itemsArray) {
                int val = 0;
                if(bitStringToInt(s.getCfg().substring(3, 6), true) < 0) {
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
            result = GroupSensors.concatenate(result, measurementBytes);
            return ReadResponse.success(resourceId, result);
        } else {
            return super.read(identity, resourceId);
        }
    }

    public static int bitStringToInt(String b, boolean isSign) {
        int value = 0;
        for (int i = 0; i < b.length(); i++) {
            if(b.charAt(i) == '1') { 
                int add = (int) Math.pow(2, (b.length() - 1 - i));
                if(isSign && i == 0) { //first sing minus
                    add *=-1;
                }
                value += add;
            }
        }
        return value;
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
