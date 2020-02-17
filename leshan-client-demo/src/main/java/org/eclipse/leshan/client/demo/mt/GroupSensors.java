package org.eclipse.leshan.client.demo.mt;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.mikrotik.iot.sd.utils.ByteUtil;
import org.mikrotik.iot.sd.utils.PredefinedEvent;
import org.mikrotik.iot.sd.utils.PushEvent;
import org.mikrotik.iot.sd.utils.CodeWrapper;
import org.mikrotik.iot.sd.utils.CustomEvent;
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

    // private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Group object"));

    private final Map<Integer, PushEvent> mEventList = new HashMap<Integer, PushEvent>();
    private final Map<Integer, String> mSerialMap = new HashMap<Integer, String>();
    private final Map<Integer, SensorReadings> mSensorMap = new HashMap<Integer, SensorReadings>();
    private final ArrayList<AlarmStatus> mAlarmStatusList = new ArrayList<AlarmStatus>();
    private final ArrayList<Devices> mDevicesList = new ArrayList<Devices>();
    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2, R3, R4, R5, R6, R7);

    private byte[] mEventConfig = new byte[0];
    private int mNotifyDelay = 300;
    private Date mLastEventRead;
    public void setGroupSensors(String... serialNrs) {
        // scheduleNext();
        int i = 0;
        for (String serial : serialNrs) {
            this.mSerialMap.put(i, serial); 
            SensorReadings t = new SensorReadings();
            t.setId(i);
            t.setGroupSensors(this);
            mSensorMap.put(i, t);

            AlarmStatus a = new AlarmStatus();
            a.setId(i);
            a.setGroupSensors(this);
            mAlarmStatusList.add(a);

            Devices d = new Devices();
            d.setSerialNumber(serial);
            d.setSensorReadings(t);
            d.setId(i);
            d.setGroupSensors(this);
            mDevicesList.add(d);
            i++;
        }
    }
    
    public void setNotifyDelay(int lifetime) {
        this.mNotifyDelay = (lifetime / 3) * 2;
    }

    // private void scheduleNext() {
    //     scheduler.schedule(new Runnable() {
    //         @Override
    //         public void run() {
    //             createPredefinedEvent();
    //             scheduleNext();
    //         }
    //     }, 10 + mRnd.nextInt(30), TimeUnit.SECONDS);
    // }

    //todo concurrent lock separate add/ clear //read
    public synchronized void pushEvents(final PushEvent... events) {
        if(events[0] != null) {
            boolean isServerNotify = false;
            for(PushEvent ev : events) {
                PushEvent eventInList = mEventList.get(ev.getId());
                if(eventInList == null) {
                    mEventList.put(ev.getId(), ev);
                    eventInList = ev;
                } else {
                    eventInList.addInstance(ev.getInstance());    
                }

                if(eventInList.isImmediateNotify() ){
                    isServerNotify = true;
                }
            }
            
            if(isServerNotify 
                && this.mLastEventRead != null
                && (this.mLastEventRead.getTime() / 1000) + this.mNotifyDelay 
                    <= (System.currentTimeMillis() / 1000)) {
                this.mLastEventRead = new Date(); //locks Registration trigger
                this.mLeshanClient.triggerRegistrationUpdate();
                LOG.info("Trigger Registration Update from EVENT!");
            } else if(isServerNotify && this.mLastEventRead != null) {
                long waitTimeSec = ((this.mLastEventRead.getTime() / 1000) + this.mNotifyDelay) - (System.currentTimeMillis() / 1000); 
                LOG.info("Trigger Registration Update skipped! WaitTimeLeft:{}", waitTimeSec);
            }
            fireResourcesChange(R0);
        } else {
            mEventList.clear();  
        }
    }

    public Map<Integer, SensorReadings> getSensorMap() {
        return mSensorMap;
    }

    public SensorReadings[] getSensorReadings() {
        SensorReadings[] arr = new SensorReadings[mSensorMap.size()];
        int pos = 0;
        for(Map.Entry<Integer, SensorReadings> entry: mSensorMap.entrySet()) {
            arr[pos] = entry.getValue();
            pos++;
        }
        return arr;
    }

    public AlarmStatus[] getAlarmReadings() {
        return mAlarmStatusList.toArray(new AlarmStatus[mAlarmStatusList.size()]);
    }

    public Devices[] getDevices() {
        return mDevicesList.toArray(new Devices[mDevicesList.size()]);
    }

    public void setLeshanClient(LeshanClient client) {
        this.mLeshanClient = client;
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            this.mLastEventRead = new Date();
            byte[] bEvent;
            //todo: concurrent execute clear and read??
            if(!mEventList.isEmpty()) {
                byte[][] concat = new byte[mEventList.size()][];
                int pos = 0;
                for(Map.Entry<Integer, PushEvent> entry: mEventList.entrySet()) {
                    concat[pos] = entry.getValue().toEventListByte();
                    pos++;
                }
                bEvent = ByteUtil.concatenate(concat);
            } else {
                bEvent = new PredefinedEvent(CodeWrapper.EventCode.NO_EVENT).toEventListByte();
            }
            return ReadResponse.success(resourceId, bEvent);
        case R1:
            return ReadResponse.success(resourceId, this.mEventConfig);
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
                //if null clear all events!
                this.pushEvents((PushEvent)null);
                return ExecuteResponse.success();
            default:
                return ExecuteResponse.notFound();
        }
    }

    @Override
    public synchronized WriteResponse write(ServerIdentity identity, int resourceId, LwM2mResource value) {
        switch (resourceId) {
        case R0:
            return super.write(identity, resourceId, value);
        case R1:
            if(value.getType().equals(ResourceModel.Type.OPAQUE)) {
                for(byte[] b : ByteUtil.split((byte[])value.getValue(), 8)) {
                    CustomEvent customEvent = new CustomEvent(b);
                    if(customEvent.isValid()) {
                        LOG.info("data:{}", customEvent.toString());
                        for(int i : customEvent.getInstance()) {
                            SensorReadings s = mSensorMap.get(i);
                            if(s != null) {
                                s.setEvent(customEvent);   
                            }   
                        }
                    } else {
                        return WriteResponse.badRequest("Corrupted configuration");
                    }
                };
                this.mEventConfig = (byte[])value.getValue();  
                return WriteResponse.success();
            } else {
                return super.write(identity, resourceId, value);
            }
        default:
            //NOT FOUND
            return super.write(identity, resourceId, value);
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
}