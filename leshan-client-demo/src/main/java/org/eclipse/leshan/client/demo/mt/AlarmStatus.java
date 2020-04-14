package org.eclipse.leshan.client.demo.mt;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.mikrotik.iot.sd.utils.PredefinedEvent;
import org.mikrotik.iot.sd.utils.CodeWrapper.EventCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;

public class AlarmStatus extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(AlarmStatus.class);
    //private static final Logger LOG = LoggerFactory.getLogger(AlarmStatus.class);
    private static final int R0 = 0;
    private static final int R1 = 1;
    private static final int R2 = 2;
    private static final int R3 = 3;


    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2, R3);
    private final ScheduledExecutorService scheduler;
    private final Random mRnd = new Random();

    private Long mAlarmStatus;
    private Boolean mHushed;
    private Boolean mCoAlarm;
    private Boolean mTemperatureAlarm;

    private GroupSensors mGroupSensors;

    public AlarmStatus() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Alarm status"));
        scheduleReadings();
        initDefault();
    }

    public void initDefault() {
        this.mAlarmStatus = 0l;
        this.mHushed = false;
        this.mCoAlarm = false;
        this.mTemperatureAlarm = false;
    }

    private void scheduleReadings() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                scheduleReadings();  
                initDefault();
                mGroupSensors.getSensorMap().get(getId()).clearTarget();
                double d = mRnd.nextDouble();
                if(d < 0.1) {
                    SensorReadings sr = mGroupSensors.getSensorMap().get(getId());
                    if(d < 0.02) {
                        mAlarmStatus = 2l;
                        sr.getSensor(SensorReadings.R0).setTarget(80d);
                        sr.getSensor(SensorReadings.R1).setTarget(10d);
                        sr.getSensor(SensorReadings.R3).setTarget(3000d);
                        fireResourcesChange(AlarmStatus.R0);
                    } else if(d < 0.04) {
                        mAlarmStatus = 1l;
                        mHushed = true;
                        sr.getSensor(SensorReadings.R3).setTarget(3000d);
                        fireResourcesChange(AlarmStatus.R0, AlarmStatus.R3);
                    } else if(d < 0.06) {
                        mCoAlarm = true;  
                        sr.getSensor(SensorReadings.R4).setTarget(80d); 
                        fireResourcesChange(AlarmStatus.R1);
                    } else {
                        mTemperatureAlarm = true;
                        sr.getSensor(SensorReadings.R0).setTarget(80d);
                        sr.getSensor(SensorReadings.R1).setTarget(10d);
                        sr.getSensor(SensorReadings.R3).setTarget(3000d);
                        fireResourcesChange(AlarmStatus.R2);
                    }
                    PredefinedEvent ev = new PredefinedEvent(EventCode.ALARM);
                    ev.addInstance(getId());
                    LOG.info("Creating Alarm EVENT:{};", ev.toString());
                    mGroupSensors.pushEvents(ev); 
                }
            }
        }, 180 + mRnd.nextInt(180), TimeUnit.SECONDS);
    }

    public void setGroupSensors(GroupSensors groupSensors) {
        this.mGroupSensors = groupSensors;
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            return ReadResponse.success(resourceId, mAlarmStatus);
        case R1:  
            return ReadResponse.success(resourceId, mCoAlarm);
        case R2:
            return ReadResponse.success(resourceId, mTemperatureAlarm);
        case R3:
            return ReadResponse.success(resourceId, mHushed);
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
