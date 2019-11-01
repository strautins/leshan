package org.eclipse.leshan.client.demo.mt;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.model.ResourceModel.Type;

public class TemperatureReadings extends SensorConfig {
    private static final Logger LOG = LoggerFactory.getLogger(TemperatureReadings.class);
    private final Random rng = new Random();
    private double mCurrentValue = 20d;
    private boolean mIsBottom = false;
    
    private List<Double> mMeasurementList = new ArrayList<Double>();

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            HashMap<Integer, Double> temperatureMap = new HashMap<Integer, Double>();
            int i = 0;
            for (double val : this.mMeasurementList) {
                temperatureMap.put(i,val); 
                i++;
            } 
            return ReadResponse.success(resourceId, temperatureMap, Type.FLOAT);
        case R1:
            return ReadResponse.success(resourceId, super.getLMT());
        default:
            return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        switch (resourceId) {
            case R2:
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
        return super.write(identity, resourceid, value);
    }
    @Override
    public void adjustMeasurements() {
        super.scheduleReadings();
        if(super.isEnable()) {
            float delta = (rng.nextInt(20) - 10) / 10f;
            this.mCurrentValue += delta;

            if(this.mCurrentValue  <= 15d || this.mMeasurementList.isEmpty() && delta < 0) {
                this.mIsBottom = true;
            } else if(this.mCurrentValue  >= 31d || this.mMeasurementList.isEmpty() && delta < 0) {
                this.mIsBottom = false;
            }
            if(this.mIsBottom ) {
                this.mCurrentValue += 0.5d;
            } else {
                this.mCurrentValue -= 0.5d;
            }
            if(GroupSensors.isFullList(this.mMeasurementList)) {
                this.mMeasurementList.remove(0);    
            }
            this.mMeasurementList.add(GroupSensors.getTwoDigitValue(this.mCurrentValue));
            super.setLMT(new Date());
            fireResourcesChange(R0, R1);
        }
    }
    public synchronized void resetMeasurementList(Date dat) {
        //get seconds
        int lmt = (int) (super.getLMT().getTime()/1000);  
        int st = (int) (dat.getTime()/1000);  
        if(lmt <= st) {
            this.mMeasurementList.clear();    
        } else {
            int left = lmt - st;
            int recLeft = left / this.getInterval(); 
            mMeasurementList.subList(0, (mMeasurementList.size() - recLeft)).clear();
        }
        fireResourcesChange(R0, R1);
    }
    public Double getCurrentReading() {
        return GroupSensors.getTwoDigitValue(this.mCurrentValue);
    }
}
