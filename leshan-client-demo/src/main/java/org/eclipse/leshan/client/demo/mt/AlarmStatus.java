package org.eclipse.leshan.client.demo.mt;

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


    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2, R3, R4, R5, R6, R7);
    private final ScheduledExecutorService scheduler;
    private Integer mInterval = 10;
    private Long mAlarmStatus = 0l;
    private Boolean mHushed = false;
    private Boolean mCoAlarm = false;
    private SensorReadings mSensorReadings;

    public AlarmStatus() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Alarm status"));
        scheduleReadings();
    }
    public void setSensors(SensorReadings s) {
        this.mSensorReadings = s;
    }
    private void scheduleReadings() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                scheduleReadings();  
                fireResourcesChange(R3,R4, R5, R6, R7);
            }
        }, mInterval, TimeUnit.SECONDS);
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            return ReadResponse.success(resourceId, mAlarmStatus);
        case R1:
            return ReadResponse.success(resourceId, mHushed);
        case R2:  
            return ReadResponse.success(resourceId, mCoAlarm);
        case R3:
            return ReadResponse.success(resourceId, mSensorReadings.getSensor(SensorReadings.R0).getCurrentValue(2));
        case R4:
            return ReadResponse.success(resourceId, mSensorReadings.getSensor(SensorReadings.R1).getCurrentValue(0));
        case R5:
            return ReadResponse.success(resourceId, mSensorReadings.getSensor(SensorReadings.R2).getCurrentValue(2));
        case R6:
            return ReadResponse.success(resourceId, mSensorReadings.getSensor(SensorReadings.R3).getCurrentValue(0));
        case R7:
            return ReadResponse.success(resourceId, mSensorReadings.getSensor(SensorReadings.R4).getCurrentValue(2)); 
        default:
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
