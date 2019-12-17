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

public class CoReadings extends SensorConfig {
    private static final Logger LOG = LoggerFactory.getLogger(CoReadings.class);
    private final Random rng = new Random();
    private Long mCurrentValue = 0l;
    private boolean mIsBottom = false;

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            HashMap<Integer, Double> temperatureMap = new HashMap<Integer, Double>();
            int i = 0;
            for (Object val : super.getMeasurementList()) {
                temperatureMap.put(i, Double.parseDouble(val.toString())); 
                i++;
            } 
            return ReadResponse.success(resourceId, temperatureMap, Type.FLOAT);
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
            int delta = rng.nextInt(2) - 1;
            this.mCurrentValue += delta;
            if(this.mCurrentValue  <= 0 || super.getMeasurementList().isEmpty() && delta < 0) {
                this.mIsBottom = true;
            } else if(this.mCurrentValue  >= 10 || super.getMeasurementList().isEmpty() && delta > 0 ) {
                this.mIsBottom = false;
            }
            if(this.mIsBottom) {
                this.mCurrentValue += 1;
            } else {
                this.mCurrentValue -= 1;
            }
            if(this.mCurrentValue < 0) {
                this.mCurrentValue = 0l;  
            }
            //with fire resource change 
            super.addMeasurementList(this.mCurrentValue);
        }
    }
    public Long getCurrentReading() {
        return this.mCurrentValue;
    }
}
