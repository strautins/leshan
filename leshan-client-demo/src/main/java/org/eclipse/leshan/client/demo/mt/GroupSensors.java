package org.eclipse.leshan.client.demo.mt;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.demo.mt.utils.ByteUtil;
import org.eclipse.leshan.client.demo.mt.utils.CustomEvent;
import org.eclipse.leshan.client.demo.mt.utils.PredefinedEvent;
import org.eclipse.leshan.client.demo.mt.utils.PredefinedEvent.PredefinedEventCode;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupSensors extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(GroupSensors.class);
    private static final int R0 = 0;
    private static final int R1 = 1;
    private static final int R2 = 2;
    private static final int R3 = 3;
    private static final int R4 = 4;
    private static final int R5 = 5;
    private static final int R6 = 6;
    private static final int R7 = 7;


    private LeshanClient mLeshanClient;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Group object"));

    private HashMap<PredefinedEventCode, PredefinedEvent> mEventList = new HashMap<PredefinedEventCode, PredefinedEvent>();
    private HashMap<Integer, String> mSerialMap = new HashMap<Integer, String>();
    private ArrayList<SensorReadings> mSensorList = new ArrayList<SensorReadings>();
    private ArrayList<AlarmStatus> mAlarmStatusList = new ArrayList<AlarmStatus>();
    private ArrayList<Devices> mDevicesList = new ArrayList<Devices>();
    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2, R3, R4, R5, R6, R7);
    private final Random mRnd = new Random();


    private int mNotifyDelay = 10;
    private Date mLastEventRead;
    public void setGroupSensors(String... serialNrs) {
        scheduleNext();
        int i = 0;
        for (String serial : serialNrs) {
            this.mSerialMap.put(i, serial); 
            SensorReadings t = new SensorReadings();
            t.setId(i);
            mSensorList.add(t);

            AlarmStatus a = new AlarmStatus();
            a.setSensors(t);
            a.setId(i);
            mAlarmStatusList.add(a);

            Devices d = new Devices();
            d.setSerialNumber(serial);
            d.setSensorReadings(t);
            d.setId(i);
            mDevicesList.add(d);
            i++;
        }
    }
    private void scheduleNext() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                createEvent();
                scheduleNext();
            }
        }, 10 + mRnd.nextInt(30), TimeUnit.SECONDS);
    }
    public void createEvent() {
        //get random event, first event = no event??
        PredefinedEventCode ev = PredefinedEvent.PREDEFINED_EVENT_VALUE.get(1 + mRnd.nextInt(10));
        PredefinedEvent e = mEventList.get(ev);
        if(e == null) {
            e = new PredefinedEvent(ev);
            mEventList.put(ev, e);
        }
        LOG.info("Creating EVENT:{}; Is Registration Update:{}", e.getCode(), e.isImmediateNotify());
        //rnd instance!
        int instance = mRnd.nextInt(mSerialMap.size());
        e.addParam2(instance);
        //todo change resource for event
        // switch(e.getId()) {
        //     case 1:
        //         this.mDevicesList.get(instance).setId(id);
        //     break;
        //     case 2:
        //         this.mDevicesList.get(instance).setId(id);
        //     break;
        //     default:
        //     // do nothing
        // }
        if(e.isImmediateNotify() && this.mLastEventRead!= null
             && (this.mLastEventRead.getTime() / 1000) + mNotifyDelay 
             <= (System.currentTimeMillis() / 1000)) {
            this.mLeshanClient.triggerRegistrationUpdate();
            LOG.info("Trigger Registration Update from EVENT!");
        }
    }

    public ArrayList<SensorReadings> getSensorReadings() {
        return this.mSensorList;
    }

    public ArrayList<AlarmStatus> getAlarmReadings() {
        return this.mAlarmStatusList;
    }

    public ArrayList<Devices> getDevices() {
        return this.mDevicesList;
    }

    public void setLeshanClient(LeshanClient client) {
        this.mLeshanClient = client;
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            this.mLastEventRead = new Date();
            byte[] bEvent = new byte[0];
            if(!mEventList.isEmpty()) {
                for(Map.Entry<PredefinedEventCode, PredefinedEvent> entry: mEventList.entrySet()) {
                    bEvent = GroupSensors.concatenate(bEvent, entry.getValue().toByte());
                }
            } else {
                bEvent = GroupSensors.concatenate(bEvent, new PredefinedEvent(PredefinedEventCode.NO_EVENT).toByte());
            }
            return ReadResponse.success(resourceId, bEvent);
        case R1:
            byte[] bCfg = new byte[0];
            return ReadResponse.success(resourceId, bCfg);
        case R2:
            return super.read(identity, resourceId);    
        case R3:
            return super.read(identity, resourceId);
        case R4:
            return super.read(identity, resourceId);
        case R5:
            return ReadResponse.success(resourceId, this.mNotifyDelay);
        case R6:
            return super.read(identity, resourceId);
        default:
            return super.read(identity, resourceId);
        }
    }

    @Override
    public ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        if (params != null && params.length() != 0) {
            System.out.println("\t params " + params);
        }
        switch (resourceId) {
            case R2:
                for(SensorReadings sensor: getSensorReadings()) {
                    sensor.clearData(params);   
                }
                //clear all events with data!
                this.mEventList.clear();
                return ExecuteResponse.success();
            default:
                return ExecuteResponse.notFound();
        }
    }

    @Override
    public synchronized WriteResponse write(ServerIdentity identity, int resourceId, LwM2mResource value) {
        switch (resourceId) {
        case R0:
            //NOT FOUND
            return super.write(identity, resourceId, value);
        case R1:
            if(value.getType().equals(ResourceModel.Type.OPAQUE)) {
                byte[] bCfg = (byte[])value.getValue();  
                StringBuilder sb = new StringBuilder();
                for(byte bs: bCfg) {
                    sb.append(String.format("%8s", Integer.toBinaryString(bs & 0xFF)).replace(' ', '0')); 
                }
                LOG.error("Received:{}", sb.toString());
                for(byte[] b : ByteUtil.split(bCfg, 8)) {
                    LOG.error("data:{}:{}", b.length, new CustomEvent(b).toString());
                };
                return WriteResponse.success();
            } else {
                return super.write(identity, resourceId, value);
            }
        default:
            //NOT FOUND
            return super.write(identity, resourceId, value);
        }
    }

    public static int byteToInt(byte[] b) {
        int value = 0;
        for (int i = 0; i < b.length; i++) {
            //value = (value << 8) + (b[i] & 0xff); //BIG_ENDIAN
            value += ((int) b[i] & 0xffL) << (8 * i); //LITTLE_ENDIAN
        }
        return value;
    }

    public static float byteArrayToFloat(byte[] bytes) {
        int intBits = byteToInt(bytes);
        return Float.intBitsToFloat(intBits);  
    }
    
    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    public static boolean isInt(String strNum) {
        try {
            Integer.parseInt(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    public static boolean isLong(String strNum) {
        try {
            Long.parseLong(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    public static boolean isBoolean(String strNum) {
        try {
            Boolean.parseBoolean(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    public static Date getDate(String value) {
        if(GroupSensors.isLong(value)) {
            // let's assume we received the seconds since 1970/1/1
            return new Date(Long.parseLong(value) * 1000); 
        } else {
            // let's assume we received an ISO 8601 format date
            try {
                DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
                XMLGregorianCalendar cal = datatypeFactory.newXMLGregorianCalendar(value);
                return cal.toGregorianCalendar().getTime();
            } catch (DatatypeConfigurationException | IllegalArgumentException e) {
                //Fri Aug 16 12:17:00 EEST 2019
                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", new Locale("us"));
                try {
                    return (Date) sdf.parse(value);
                } catch (ParseException e1) {
                    return null;
				}
            }
        }
    }
    
    public static Date getDate(LwM2mResource value) {
       return getDate(value.getValue().toString());
    }
    
    public static double getDigitValue(double value, int round) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(round, RoundingMode.HALF_UP).doubleValue();
    }

    public static Boolean isIntervalValid(String value) {
        Boolean result = false;
        if(isInt(value)) {
            result = isIntervalValid(Integer.parseInt(value));
        }
        return result;
    } 
    
    public static Boolean isIntervalValid(Integer value) {
        Boolean result = false;
        if(value > 0 && value <= 120) {
            result = true;
        }
        return result;
    } 

    public static <T> Boolean isFullList(List<T> list) {
        Boolean result = false;
        if(list != null && list.size() > 100) {
            result = true;
        }
        return result;
    } 
    
    public static byte[] concatenate(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
    
        byte[] c = (byte[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }
}