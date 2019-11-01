package org.eclipse.leshan.client.demo.mt;

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
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupSensors extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(GroupSensors.class);
    private static final int mR0 = 0;
    private static final int mR1 = 1;
    private static final int mR2 = 2;
    private static final int mR3 = 3;
    private static final int mR4 = 4;
    private static final int mR5 = 5;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Group object"));
    private int mInstanceCount = -1;
    private HashMap<Integer, String> mSerialMap = new HashMap<Integer, String>();
    private ArrayList<TemperatureReadings> mTemperatureList = new ArrayList<TemperatureReadings>();
    private ArrayList<HumidityReadings> mHumidityList = new ArrayList<HumidityReadings>();
    private ArrayList<AlarmStatus> mAlarmStatusList = new ArrayList<AlarmStatus>();
    private ArrayList<Co2Readings> mCo2List = new ArrayList<Co2Readings>();
    private ArrayList<AtmosphericPressureReadings> mAtmosphericList = new ArrayList<AtmosphericPressureReadings>();
    private ArrayList<CoReadings> mCoList = new ArrayList<CoReadings>();
    private ArrayList<Devices> mDevicesList = new ArrayList<Devices>();
    private static final List<Integer> supportedResources = Arrays.asList(mR0, mR1, mR2, mR3, mR4, mR5);
    /**Alarm triggered */
    private boolean mR1Value = false;
    /**device info changed */
    private boolean mR2Value = false;
    private long mR5Value = 30;
    public void setGroupSensors(String... serialNrs) {
        scheduleNext();
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

            Devices d = new Devices();
            d.setSerialNumber(serial);
            d.setTemperature(t);
            d.setHumidity(h);
            d.setCo(co);
            d.setCo2(c);
            d.setPressure(at);
            d.setId(mInstanceCount);
            mDevicesList.add(d);
        }
    }
    private void scheduleNext() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                scheduleNext();
                fireResourcesChange(0);
            }
        }, 5, TimeUnit.SECONDS);
    }
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
    public ArrayList<Devices> getDevices() {
        return this.mDevicesList;
    }
    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        switch (resourceid) {
        case mR0:
            return ReadResponse.success(resourceid, (this.mR1Value || this.mR2Value));
        case mR1:
            return ReadResponse.success(resourceid, this.mR1Value);
        case mR2:
            return ReadResponse.success(resourceid, this.mR2Value);
        case mR5:
            return ReadResponse.success(resourceid, this.mR5Value);
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
        case mR0:
            //NOT FOUND
            return super.write(identity, resourceid, value);
        case mR1:
            if(isBoolean(value.getValue().toString())) {
                this.mR1Value = Boolean.parseBoolean(value.getValue().toString());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            }
        case mR2:
            if(isBoolean(value.getValue().toString())) {
                this.mR2Value = Boolean.parseBoolean(value.getValue().toString());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            }
        case mR5:
            if(isInt(value.getValue().toString())) {
                this.mR5Value = Integer.parseInt(value.getValue().toString());
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
            // let's assume we received the millisecond since 1970/1/1
            return new Date(Long.parseLong(value)); 
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