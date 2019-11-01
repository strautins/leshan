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

public class Co2Readings extends SensorConfig {
    private static final Logger LOG = LoggerFactory.getLogger(Co2Readings.class);

    private final Random rng = new Random();
    private Long mCurrentValue = 600l;
    private boolean mIsBottom = false;
    
    private List<Long> mMeasurementList = new ArrayList<Long>();

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            HashMap<Integer, Long> temperatureMap = new HashMap<Integer, Long>();
            int i = 0;
            for (Long val : this.mMeasurementList) {
                temperatureMap.put(i,val); 
                i++;
            } 
            return ReadResponse.success(resourceId, temperatureMap, Type.INTEGER);
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
        scheduleReadings();
        if(super.isEnable()) {
            //System.out.println("Temperature sensor write!");
            int delta = rng.nextInt(100) - 50;
            this.mCurrentValue += delta;
            if(this.mCurrentValue  <= 450 || this.mMeasurementList.isEmpty() && delta < 0) {
                this.mIsBottom = true;
            } else if(this.mCurrentValue  >= 1800 || this.mMeasurementList.isEmpty() && delta > 0 ) {
                this.mIsBottom = false;
            }
            if(this.mIsBottom) {
                this.mCurrentValue += 15;
            } else {
                this.mCurrentValue -= 15;
            }
            if(GroupSensors.isFullList(this.mMeasurementList)) {
                this.mMeasurementList.remove(0);    
            }
            this.mMeasurementList.add(this.mCurrentValue);
            super.setLMT(new Date());
            fireResourcesChange(R0, R1);
        }
    }
    public synchronized void resetMeasurementList(Date dat) {
        int lmt = (int) (super.getLMT().getTime() / 1000);  
        int st = (int) (dat.getTime() / 1000);  
        if(lmt <= st) {
            this.mMeasurementList.clear();    
        } else {
            int left = lmt - st;
            int recLeft = left / super.getInterval(); 
            mMeasurementList.subList(0, (mMeasurementList.size() - recLeft)).clear();
        }
        fireResourcesChange(R0, R1);
    }
    
    public Long getCurrentReading() {
        return this.mCurrentValue;
    }
}
