package org.eclipse.leshan.client.demo.mt;

import java.util.Date;
import java.util.HashMap;
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

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            HashMap<Integer, Long> temperatureMap = new HashMap<Integer, Long>();
            int i = 0;
            for (Object val : super.getMeasurementList()) {
                temperatureMap.put(i, (Long)val); 
                i++;
            } 
            return ReadResponse.success(resourceId, temperatureMap, Type.INTEGER);
        case R1:
            return ReadResponse.success(resourceId, super.getFMT());
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
    public synchronized WriteResponse write(ServerIdentity identity, int resourceId, LwM2mResource value) {
        return super.write(identity, resourceId, value);
    }

    @Override
    public void adjustMeasurements() {
        if(super.isEnable()) {
            int delta = rng.nextInt(100) - 50;
            this.mCurrentValue += delta;
            if(this.mCurrentValue  <= 450 || super.getMeasurementList().isEmpty() && delta < 0) {
                this.mIsBottom = true;
            } else if(this.mCurrentValue  >= 1800 || super.getMeasurementList().isEmpty() && delta > 0 ) {
                this.mIsBottom = false;
            }
            if(this.mIsBottom) {
                this.mCurrentValue += 15;
            } else {
                this.mCurrentValue -= 15;
            }
             //with fire resource change 
             super.addMeasurementList(this.mCurrentValue);
        }
    }
    
    public Long getCurrentReading() {
        return this.mCurrentValue;
    }
}
