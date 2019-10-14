package org.eclipse.leshan.client.demo.mt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;

public class AlarmStatus extends BaseInstanceEnabler {
    
    //private static final Logger LOG = LoggerFactory.getLogger(AlarmStatus.class);
    private static final int R0 = 0;
    private static final int R1 = 1;
    private static final int R2 = 2;
    private static final int R3 = 3;
    private static final int R4 = 4;
    private static final int R5 = 5;
    private static final int R6 = 6;
    private static final int R7 = 7;
    private static final int R8 = 8;


    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2, R3, R4, R5, R6, R7, R8);
    // private final ScheduledExecutorService scheduler;
    // private Integer mInterval = 5;
    private Long mAlarmStatus = 0l;
    private Boolean mHushed = false;
    private Boolean mTemperatureAlarm = false;
    private Boolean mCoAlarm = false;
    private Co2Readings mCo2;
    private HumidityReadings mHumidityReadings;
    private TemperatureReadings mTemperatureReadings;
    private AtmosphericPressureReadings mAtmosphericPressureReadings;
    private CoReadings mCoReadings;

    public AlarmStatus() {
        // this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Alarm status"));
        // scheduleReadings();
    }
    public void setSensors(Co2Readings co2, HumidityReadings hm, TemperatureReadings t, AtmosphericPressureReadings at, CoReadings co) {
        this.mCo2 = co2;
        this.mHumidityReadings = hm;
        this.mTemperatureReadings = t;
        this.mAtmosphericPressureReadings = at;
        this.mCoReadings = co;
    }
    // private void scheduleReadings() {
    //     scheduler.schedule(new Runnable() {
    //         @Override
    //         public void run() {
    //             scheduleReadings();  
    //             fireResourcesChange(R4, R5, R6, R7, R8);
    //         }
    //     }, mInterval, TimeUnit.SECONDS);
    // }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            return ReadResponse.success(resourceId, mAlarmStatus);
        case R1:
            return ReadResponse.success(resourceId, mHushed);
        case R2:  
            return ReadResponse.success(resourceId, mTemperatureAlarm);
        case R3:
            return ReadResponse.success(resourceId, mCoAlarm);
        case R4:
            return ReadResponse.success(resourceId, mTemperatureReadings.getCurrentReading());
        case R5:
            return ReadResponse.success(resourceId, mCo2.getCurrentReading());
        case R6:
            return ReadResponse.success(resourceId, mCoReadings.getCurrentReading());
        case R7:
            return ReadResponse.success(resourceId, mHumidityReadings.getCurrentReading());
        case R8:
            return ReadResponse.success(resourceId, mAtmosphericPressureReadings.getCurrentReading()); 
        default:
            return super.read(identity, resourceId);
        }
    }
    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        return super.execute(identity, resourceId, params);
    }

    @Override
    public synchronized WriteResponse write(ServerIdentity identity, int resourceid, LwM2mResource value) {
        return super.write(identity, resourceid, value);
    }
    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
