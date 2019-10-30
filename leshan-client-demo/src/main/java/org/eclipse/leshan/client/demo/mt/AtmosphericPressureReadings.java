package org.eclipse.leshan.client.demo.mt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.model.ResourceModel.Type;

public class AtmosphericPressureReadings extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(AtmosphericPressureReadings.class);
    private static final int R0 = 0;
    private static final int R1 = 1;
    private static final int R2 = 2;
    private static final int R3 = 3;
    private static final int R4 = 4;
    
    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2, R3, R4);
    private final ScheduledExecutorService scheduler;
    private final Random rng = new Random();
    private double mCurrentValue = 1000d;
    private boolean mIsBottom = false;
    private boolean mIsEnable = true;
    private Integer mInterval = 10;
    
    private List<Double> mMeasurementList = new ArrayList<Double>();
    private Date mLMT = new Date();

    public AtmosphericPressureReadings() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Atmospheric Sensor"));
        scheduleReadings();
    }
    private void scheduleReadings() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                adjustMeasurements();
            }
        }, mInterval, TimeUnit.SECONDS);
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            return ReadResponse.success(resourceId, mIsEnable);
        case R1:
            return ReadResponse.success(resourceId, mInterval);
        case R2:
        
            HashMap<Integer, Double> temperatureMap = new HashMap<Integer, Double>();
            int i = 0;
            for (double val : this.mMeasurementList) {
                temperatureMap.put(i,val); 
                i++;
            } 
            return ReadResponse.success(resourceId, temperatureMap, Type.FLOAT);
        case R3:
            return ReadResponse.success(resourceId, mLMT);
        default:
            return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        switch (resourceId) {
            case R4:
                Date d = null;
                if(params == null) {
                    d = new Date();     
                } else { 
                    d = GroupSensors.getDate(params);
                }
                resetMeasurementList(d);
                return ExecuteResponse.success();
            default:
                return super.execute(identity, resourceId, params);
        }
    }

    @Override
    public synchronized WriteResponse write(ServerIdentity identity, int resourceid, LwM2mResource value) {
        LOG.info("Write on Device Resource " + resourceid + " value " + value);
        switch (resourceid) {
        case R0:
            if(GroupSensors.isBoolean(value.getValue().toString())) {
                boolean oldValue = this.mIsEnable;
                this.mIsEnable = Boolean.parseBoolean(value.getValue().toString());
                fireResourcesChange(resourceid);
                if(this.mIsEnable && !oldValue) {
                    resetMeasurementList(new Date());    
                }
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            }       
        case R1:
            if(GroupSensors.isIntervalValid(value.getValue().toString())) {
                this.mInterval = Integer.parseInt(value.getValue().toString());
                fireResourcesChange(resourceid);
                resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            }
        default:
            return super.write(identity, resourceid, value);
        }
    }

    private void adjustMeasurements() {
        scheduleReadings();
        if(this.mIsEnable) {
            //System.out.println("Temperature sensor write!");
            float delta = (rng.nextInt(20) - 10) / 10f;
            this.mCurrentValue += delta;

            if(this.mCurrentValue  <= 990d || this.mMeasurementList.isEmpty() && delta < 0) {
                this.mIsBottom = true;
            } else if(this.mCurrentValue  >= 1020 || this.mMeasurementList.isEmpty() && delta < 0) {
                this.mIsBottom = false;
            }
            if(this.mIsBottom ) {
                this.mCurrentValue += 0.5d;
            } else {
                this.mCurrentValue -= 0.5d;
            }
            
            this.mLMT = new Date();
            if(GroupSensors.isFullList(this.mMeasurementList)) {
                this.mMeasurementList.remove(0);    
            }
            this.mMeasurementList.add(GroupSensors.getTwoDigitValue(this.mCurrentValue));
            this.mLMT = new Date();
            fireResourcesChange(R2, R3);
        }
    }
    private synchronized void resetMeasurementList(Date dat) {
        //get seconds
        int lmt = (int) (mLMT.getTime()/1000);  
        int st = (int) (dat.getTime()/1000);  
        if(lmt <= st) {
            this.mMeasurementList.clear();    
        } else {
            int left = lmt - st;
            int recLeft = left / this.mInterval; 
            mMeasurementList.subList(0, (mMeasurementList.size() - recLeft)).clear();
        }
        fireResourcesChange(R2, R4);
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
    public Double getCurrentReading() {
        return GroupSensors.getTwoDigitValue(this.mCurrentValue);
    }
}
