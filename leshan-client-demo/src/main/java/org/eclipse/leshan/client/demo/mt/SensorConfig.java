package org.eclipse.leshan.client.demo.mt;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ObjectModel;

abstract class SensorConfig extends BaseInstanceEnabler {
    
    private final ScheduledExecutorService scheduler;
    
    public static final int R0 = 0;
    public static final int R1 = 1;
    public static final int R2 = 2;
    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2);
    private boolean mIsEnable = true;
    private int mInterval = 60;
    private Date mLMT = new Date();

    public SensorConfig() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Sensor"));
        scheduleReadings();
    }
    public void scheduleReadings() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                adjustMeasurements();
            }
        }, mInterval, TimeUnit.SECONDS);
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

    public Date getLMT() {
        return this.mLMT;
    }

    public void setLMT(Date value) {
        this.mLMT = value;
    }
    
    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    abstract void resetMeasurementList(Date dat);

    abstract void adjustMeasurements();
}
