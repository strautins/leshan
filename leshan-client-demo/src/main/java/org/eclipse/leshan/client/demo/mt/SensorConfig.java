package org.eclipse.leshan.client.demo.mt;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SensorConfig {
    
    private static final Logger LOG = LoggerFactory.getLogger(SensorConfig.class);
    
    private final ScheduledExecutorService scheduler;
    private int mInterval;
    private Date mFMT = new Date();

    private final Random mRnd = new Random();
    private boolean mIsBottom = false;

    private double mCurrentValue;
    private double mLow;
    private double mHigh;
    private double mDelta;
    private double mAdjust;

    private String mCfg;

    private List<Double> mMeasurementList = new ArrayList<Double>();

    public SensorConfig(double low, double high, double delta, double adjust, String cfg) {
        this.mLow = low;
        this.mHigh = high;
        this.mCurrentValue = low + (mRnd.nextDouble() * (high - low));
        this.mDelta = delta;
        this.mAdjust = adjust;
        this.mInterval = 10 + mRnd.nextInt(50);
        //3 bits value byte size 
        //3 bits value for floating point
        //1 bit is more data
        //1 bit reserved
        this.mCfg = cfg;
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
        return this.mInterval > 0;
    }

    public int getInterval() {
        return this.mInterval;
    }

    public String getCfg() {
        return this.mCfg;
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

    public List<Double> getMeasurementList() {
        return mMeasurementList;
    }

    public void addMeasurementList(Double value) {
        if(GroupSensors.isFullList(this.mMeasurementList)) {
            mMeasurementList.remove(0);  
            this.pushFMT(1);
        }
        this.mMeasurementList.add(value);
        if(this.mMeasurementList.size() == 1) {
            this.mFMT = new Date();
        }
    }

    public synchronized void resetMeasurementList(Date dat) {
        //get seconds
        if(this.mFMT != null) {
            int fmt = (int) (this.mFMT.getTime() / 1000);  
            int lmt = fmt + (mMeasurementList.size() * this.mInterval);
            int clearTime = (int) (dat.getTime() / 1000);  
            if(lmt <= clearTime) {
                this.mMeasurementList.clear(); 
                //resource null = internal error  
                //this.mFMT = null;   
            } else { 
                int left = clearTime - fmt;
                int removeCount = left / this.getInterval();
                //move first measurement time 
                pushFMT(removeCount);
                mMeasurementList.subList(0, removeCount).clear();
            }
        }
    }
    private void scheduleNext() {
        scheduleReadings(this.mInterval);
        adjustMeasurements();
    }

    public void adjustMeasurements() {
        if(this.isEnable()) {
            double delta = (mRnd.nextDouble() - 0.5) * this.mDelta;
            this.mCurrentValue += delta;

            if(this.mCurrentValue  <= this.mLow || this.getMeasurementList().isEmpty() && delta < 0) {
                this.mIsBottom = true;
            } else if(this.mCurrentValue  >= this.mHigh || this.getMeasurementList().isEmpty() && delta < 0) {
                this.mIsBottom = false;
            }
            if(this.mIsBottom ) {
                this.mCurrentValue += mAdjust;
            } else {
                this.mCurrentValue -= mAdjust;
            }
            this.addMeasurementList(this.mCurrentValue);
        }
    }
    public double getCurrentValue(int round) {
        return GroupSensors.getDigitValue(this.mCurrentValue, round);
    }
}
