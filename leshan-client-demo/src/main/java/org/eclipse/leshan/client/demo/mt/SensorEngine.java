package org.eclipse.leshan.client.demo.mt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.mikrotik.iot.sd.utils.ByteUtil;
import org.mikrotik.iot.sd.utils.CustomEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SensorEngine {
    
    private static final Logger LOG = LoggerFactory.getLogger(SensorEngine.class);
    private SensorReadings mSensorReadings;
    private final List<CustomEvent> mCustomEvents = Collections.synchronizedList(new ArrayList<CustomEvent>());
    private final ScheduledExecutorService mScheduler;
    private int mInterval;
    private Date mFMT = new Date();

    private final Random mRnd = new Random();
    private boolean mIsBottom = false;

    private Double mTarget;
    private double mCurrentValue;
    private double mLow;
    private double mHigh;
    private double mDelta;
    private double mAdjust;

    private String mCfg;

    private List<Double> mMeasurementList = new ArrayList<Double>();

    public SensorEngine(double low, double high, double delta, double adjust, String cfg) {
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
        this.mScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Sensor"));
        scheduleReadings(2); //start adjust values with delay
    }

    public void scheduleReadings(int interval) {
        mScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                scheduleNext();
            }
        }, interval, TimeUnit.SECONDS);
    }

    public void setSensorReadings(SensorReadings sensorReadings) {
        this.mSensorReadings = sensorReadings;
    }

    public void setTarget(Double value) {
        this.mTarget = value;
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

    public void setEvent(CustomEvent customEvent) {
        this.mCustomEvents.add(customEvent);
    }

    public void clearEvent() {
        this.mCustomEvents.clear();
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

            if(mTarget != null) {
                delta = (this.mTarget - this.mCurrentValue) * mRnd.nextDouble();      
            }
           
            if(this.mCurrentValue + delta <= this.mLow || this.getMeasurementList().isEmpty() && delta < 0) {
                this.mIsBottom = true;
            } else if(this.mCurrentValue + delta >= this.mHigh || this.getMeasurementList().isEmpty() && delta < 0) {
                this.mIsBottom = false;
            }

            delta += this.mIsBottom ? mAdjust : -mAdjust;

            pushEvent(mCurrentValue, delta);

            this.mCurrentValue += delta;

            this.addMeasurementList(this.mCurrentValue);
        }
    }

    private void pushEvent(double currentValue, double delta) {
        List<CustomEvent> newEvents = new ArrayList<CustomEvent>(); 
        for (CustomEvent ev : mCustomEvents) {
            boolean isEventTriggered = false;
            if((ev.getEventTriggerType().equals(CustomEvent.EventTriggerType.UP) ||
                    ev.getEventTriggerType().equals(CustomEvent.EventTriggerType.BOTH)) 
                && currentValue < ev.getValue() &&  ev.getValue() <= currentValue + delta
             ||
                (ev.getEventTriggerType().equals(CustomEvent.EventTriggerType.UP) ||
                    ev.getEventTriggerType().equals(CustomEvent.EventTriggerType.BOTH)) 
                && currentValue > ev.getValue() &&  ev.getValue() >= currentValue + delta) {
                isEventTriggered = true; 
            }

            if(isEventTriggered) {
                newEvents.add(new CustomEvent(ev.getEventCode(), 
                    ev.getEventTriggerType(), ev.isImmediateNotify(),
                    ev.getValue(), this.mSensorReadings.getId()));
            }
            LOG.info("Adjust:{}; delta:{}; sensor:{} ~ {}; value:{}; {}", 
                ev.getEventTriggerType().name(), 
                ByteUtil.getDoubleRound(delta, 2), 
                ByteUtil.getDoubleRound(currentValue, 2), 
                ByteUtil.getDoubleRound(currentValue + delta, 2), 
                ev.getValue(), isEventTriggered);
        }

        if(newEvents.size() > 0) {
            this.mSensorReadings.getGroupSensors().pushEvents(newEvents.toArray(new CustomEvent[newEvents.size()]));    
        }
    }

    public double getCurrentValue(int round) {
        return ByteUtil.getDoubleRound(this.mCurrentValue, round);
    }
}
