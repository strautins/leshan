package org.eclipse.leshan.client.demo.mt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ObjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class SensorConfig extends BaseInstanceEnabler {
    
    private static final Logger LOG = LoggerFactory.getLogger(TemperatureReadings.class);
    
    private final ScheduledExecutorService scheduler;
    
    public static final int R0 = 0;
    public static final int R1 = 1;
    public static final int R2 = 2;
    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2);
    private boolean mIsEnable = true;
    private int mInterval = 30;
    private Date mFMT = new Date();

    private List<Object> mMeasurementList = new ArrayList<Object>();

    public SensorConfig() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Sensor"));
        scheduleReadings(2); //start adjust values with delay
    }
    public void scheduleReadings(int interval) {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                scheduleNext();
            }
        }, interval, TimeUnit.SECONDS);
    }

    public boolean isEnable() {
        return this.mIsEnable;
    }

    public void setEnable(boolean value) {
        this.mIsEnable = value;
    }

    public int getInterval() {
        return this.mInterval;
    }

    public void setInterval(int value) {
        this.mInterval = value;
    }

    public Date getFMT() {
        return this.mFMT;
    }

    public void setFMT(Date value) {
        this.mFMT = value;
    }

    public void pushFMT(int qty) {
        this.mFMT = new Date(mFMT.getTime() + (this.mInterval * qty * 1000));
    }
    
    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
    public List<Object> getMeasurementList() {
        return mMeasurementList;
    }

    public void addMeasurementList(Object value) {
        if(GroupSensors.isFullList(this.mMeasurementList)) {
            mMeasurementList.remove(0);  
            this.pushFMT(1);
        }
        this.mMeasurementList.add(value);
        if(this.mMeasurementList.size() == 1) {
            this.mFMT = new Date();
            fireResourcesChange(R0, R1);
        } else {
            fireResourcesChange(R0);
        }
    }

    public synchronized void resetMeasurementList(Date dat) {
        //get seconds
        if(this.mFMT != null) {
            int fmt = (int) (this.mFMT.getTime() / 1000);  
            int lmt = fmt + (mMeasurementList.size() * this.mInterval);
            int clearTime = (int) (dat.getTime() / 1000);  
            if(lmt <= clearTime) {
                LOG.warn("{} Clear all; {} : {}",super.getId(), fmt, lmt);   
                this.mMeasurementList.clear(); 
                //resource null = internal error  
                //this.mFMT = null;   
            } else { 
                int left = clearTime - fmt;
                int removeCount = left / this.getInterval();
                //move first measurement time 
                pushFMT(removeCount);
                mMeasurementList.subList(0, removeCount).clear();
                
                LOG.warn("{} Clear part; {} : {} : {} : {} : {}",super.getId(), fmt, clearTime, lmt, removeCount, left);  
                //if(mMeasurementList.size() == 0) {
                //   this.mFMT = null;      
                //}
            }
            fireResourcesChange(R0, R1);
        }
    }
    private void scheduleNext() {
        scheduleReadings(this.mInterval);
        adjustMeasurements();
    }

    abstract void adjustMeasurements();
}
