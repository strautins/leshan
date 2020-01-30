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
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ObjectModel;
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

    private static final Map<Integer, Event> EVENTS;
    static {
        Map<Integer, Event> ev = new HashMap<Integer, Event>();
        ev.put(0, new Event(0, "NO_EVENT", false, true));
        ev.put(1, new Event(1, "CRIT_BAT", true, true));
        ev.put(2, new Event(2, "BATTERY_PROB", true, true));
        ev.put(3, new Event(3, "NOT_REACHABLE", true, true));
        ev.put(4, new Event(4, "DOCKED", false, true));
        ev.put(5, new Event(5, "UNDOCKED", false, true));
        ev.put(6, new Event(6, "EXT_PWR_ON", true, true));
        ev.put(7, new Event(7, "EXT_PWR_OFF", true, true));
        ev.put(8, new Event(8, "LOW_BT_SIGNAL", false, true));
        ev.put(9, new Event(9, "LOW_CEL_SIGNAL", false, true));
        ev.put(10, new Event(10, "ALARM", true, true));

        ev.put(128, new Event(128, "TEMP_EVENT", false, false));
        ev.put(129, new Event(129, "HUMID_EVENT", false, false));
        ev.put(130, new Event(130, "PRESS_EVENT", false, false));
        ev.put(131, new Event(131, "CO2_EVENT", false, false));
        ev.put(132, new Event(132, "IAQ_EVEN", false, false));
        EVENTS = Collections.unmodifiableMap(ev);
    }

    private LeshanClient mLeshanClient;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Group object"));

    private HashMap<Integer, Event> mEventList = new HashMap<Integer, Event>();
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
        int event = 1 + mRnd.nextInt(10);
        Event e = mEventList.get(event);
        if(e == null) {
            e = EVENTS.get(event).getEvent();
            mEventList.put(e.getId(), e);
        }
        LOG.info("Creating EVENT:{}; Is Registration Update:{}", e.getCode(), e.isRegisterToServer());
        //rnd instance!
        int instance = mRnd.nextInt(mSerialMap.size());
        e.setInstance(instance);

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
        if(e.isRegisterToServer() && this.mLastEventRead!= null
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
            byte[] b = new byte[0];
            if(!mEventList.isEmpty()) {
                for(Map.Entry<Integer, Event> entry: mEventList.entrySet()) {
                    b = GroupSensors.concatenate(b, entry.getValue().getByte());
                }
            } else {
                b = GroupSensors.concatenate(b, EVENTS.get(0).getByte());
               
            }
            return ReadResponse.success(resourceId, b);
        case R1:
            //todo return set events
            return super.read(identity, resourceId);
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
    public synchronized WriteResponse write(ServerIdentity identity, int resourceid, LwM2mResource value) {
        switch (resourceid) {
        case R0:
            //NOT FOUND
            return super.write(identity, resourceid, value);
        default:
            //NOT FOUND
            return super.write(identity, resourceid, value);
        }
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