package org.eclipse.leshan.client.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;

public class GroupSensors extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(GroupSensors.class);
    //private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Group object"));
    private int mInstanceCount = -1;
    private HashMap<Integer, String> mSerialMap = new HashMap<Integer, String>();
    private ArrayList<TemperatureReadings> mTemperatureList = new ArrayList<TemperatureReadings>();
    private ArrayList<HumidityReadings> mHumidityList = new ArrayList<HumidityReadings>();
    private ArrayList<AlarmStatus> mAlarmStatusList = new ArrayList<AlarmStatus>();
    private ArrayList<Co2Readings> mCo2List = new ArrayList<Co2Readings>();
    private ArrayList<AtmosphericPressureReadings> mAtmosphericList = new ArrayList<AtmosphericPressureReadings>();
    private ArrayList<CoReadings> mCoList = new ArrayList<CoReadings>();
    private static final List<Integer> supportedResources = Arrays.asList(0, 1);
    private boolean mStatus = true;
    public void setGroupSensors(String... serialNrs) {
        //scheduleNext();
        for (String serial : serialNrs) {
            mInstanceCount++;
            this.mSerialMap.put(mInstanceCount, serial); 
            TemperatureReadings t = new TemperatureReadings();
            t.setId(mInstanceCount);
            mTemperatureList.add(t);

            HumidityReadings h = new HumidityReadings();
            h.setId(mInstanceCount);
            mHumidityList.add(h);

            Co2Readings c = new Co2Readings();
            c.setId(mInstanceCount);
            mCo2List.add(c);

            AtmosphericPressureReadings at = new AtmosphericPressureReadings();
            at.setId(mInstanceCount);
            mAtmosphericList.add(at);

            CoReadings co = new CoReadings();
            co.setId(mInstanceCount);
            mCoList.add(co);

            AlarmStatus a = new AlarmStatus();
            a.setSensors(c, h, t, at, co);
            a.setId(mInstanceCount);
            mAlarmStatusList.add(a);

            t.setAlarm(a);
            h.setAlarm(a);
            c.setAlarm(a);
            at.setAlarm(a);
            co.setAlarm(a);
        }
    }
    // private void scheduleNext() {
    //     scheduler.schedule(new Runnable() {
    //         @Override
    //         public void run() {
    //             scheduleNext();
    //             LOG.info("Schedule next !!");
    //             fireResourcesChange(0);
    //         }
    //     }, 10, TimeUnit.SECONDS);
    // }
    public ArrayList<TemperatureReadings> getTemperatureObjReadings() {
        return this.mTemperatureList;
    }
    public ArrayList<HumidityReadings> getHumidityObjReadings() {
        return this.mHumidityList;
    }
    public ArrayList<Co2Readings> getCo2ObjReadings() {
        return this.mCo2List;
    }
    public ArrayList<AlarmStatus> getAlarmReadings() {
        return this.mAlarmStatusList;
    }
    public ArrayList<AtmosphericPressureReadings> getAtmosphericReadings() {
        return this.mAtmosphericList;
    }
    public ArrayList<CoReadings> getCoReadings() {
        return this.mCoList;
    }
    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        //LOG.info("Read on Device Resource " + resourceid);
        switch (resourceid) {
        case 0:
            return ReadResponse.success(resourceid, this.mStatus);
        case 1:
            return ReadResponse.success(resourceid, mSerialMap, Type.STRING);
        default:
            return super.read(identity, resourceid);
        }
    }

    @Override
    public ExecuteResponse execute(ServerIdentity identity, int resourceid, String params) {
        //LOG.info("Execute on Device resource " + resourceid);
        if (params != null && params.length() != 0) {
            System.out.println("\t params " + params);
        }
        return ExecuteResponse.success();
    }

    @Override
    public synchronized WriteResponse write(ServerIdentity identity, int resourceid, LwM2mResource value) {
        //LOG.info("Write on Device Resource " + resourceid + " value " + value);
        switch (resourceid) {
        case 0:
            if(isBoolean(value.getValue().toString())) {
                this.mStatus = Boolean.parseBoolean(value.getValue().toString());
                fireResourcesChange(resourceid);
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            }
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
            int i = Integer.parseInt(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }
    public static boolean isBoolean(String strNum) {
        try {
            boolean i = Boolean.parseBoolean(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }
    public static Date getDate(LwM2mResource value) {
        if(GroupSensors.isInt(value.getValue().toString())) {
            // let's assume we received the millisecond since 1970/1/1
            return new Date(Integer.parseInt(value.getValue().toString()));
        } else {
            // let's assume we received an ISO 8601 format date
            try {
                DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
                XMLGregorianCalendar cal = datatypeFactory.newXMLGregorianCalendar(value.getValue().toString());
                return cal.toGregorianCalendar().getTime();
            } catch (DatatypeConfigurationException | IllegalArgumentException e) {
                //Fri Aug 16 12:17:00 EEST 2019
                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", new Locale("us"));
                try {
                    return (Date) sdf.parse(value.getValue().toString());
                } catch (ParseException e1) {
                    return null;
				}
            }
        }
    }
    
    public static double getTwoDigitValue(double value) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(2, RoundingMode.HALF_UP).doubleValue();
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