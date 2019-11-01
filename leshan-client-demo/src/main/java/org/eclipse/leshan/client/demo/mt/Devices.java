package org.eclipse.leshan.client.demo.mt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.InstanceNotFoundException;

import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;

public class Devices extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(Co2Readings.class);
    private static final int R0 = 0;
    private static final int R1 = 1;
    private static final int R2 = 2;
    private static final int R3 = 3;
    private static final int R4 = 4;
    private static final int R5 = 5;
    private static final int R6 = 6;
    private static final int R7 = 7;
    private static final int R8 = 8;
    private static final int R9 = 9;
    private static final int R10 = 10;
    private static final int R11 = 11;
    private static final int R12 = 12;
    private static final int R13 = 13;
    private static final int R14 = 14;
    private static final int R15 = 15;
    private static final int R16 = 16;
    private static final int R17 = 17;
    private static final int R18 = 18;
    private static final int R19 = 19;
    private static final int R20 = 20;

    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2, R3, R4, R5, R6, R7, R8, R9,
        R10, R11, R12, R13, R14, R15, R16, R17, R18, R19, R20);
    private final ScheduledExecutorService scheduler;
    private Integer mInterval = 60;

    private String R0Value = "1.2.0";
    private String R1Value = "1.1.0r2";
    private String R2Value= "";
    private Boolean R3Value= true;
    /**Last Active Time */
    private Date R4Value = new Date();
    private long R5Value = -67l;
    private long R6Value = 0l;
    private double R7Value = 1.2321;
    private Map<Integer, Boolean> R9Values= new HashMap<Integer, Boolean>();

    private SensorConfig temperature = null;
    private SensorConfig humidity = null;
    private SensorConfig co = null;
    private SensorConfig co2 = null;
    private SensorConfig pressure = null;
    public Devices() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Devices"));
        R9Values.put(0, true);
        R9Values.put(2, false);
        pulse();
    }
    private void pulse() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                adjustValues();
            }
        }, mInterval, TimeUnit.SECONDS);
        //fireResourcesChange(R0, R1, R2, R3, R4, R5, R6, R7, R8);
    }
    private void adjustValues() {
        pulse();
        this.R4Value = new Date();
        fireResourcesChange(R3);
    }
    public void setSerialNumber(String serialNr) {
        this.R2Value = serialNr;      
    }
    public void setTemperature(SensorConfig value) {
        this.temperature = value;      
    }
    public void setHumidity(SensorConfig value) {
        this.humidity = value;      
    }
    public void setCo(SensorConfig value) {
        this.co = value;      
    }
    public void setCo2(SensorConfig value) {
        this.co2 = value;      
    }
    public void setPressure(SensorConfig value) {
        this.pressure = value;      
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case R0:
            return ReadResponse.success(resourceId, R0Value);
        case R1:
            return ReadResponse.success(resourceId, R1Value);
        case R2:
            return ReadResponse.success(resourceId, R2Value);
        case R3:
            return ReadResponse.success(resourceId, R3Value);
        case R4:
            return ReadResponse.success(resourceId, R4Value);
        case R5:
            return ReadResponse.success(resourceId, R5Value);
        case R6:
            return ReadResponse.success(resourceId, R6Value);
        case R7:
            return ReadResponse.success(resourceId, R7Value);
        case R9:
            return ReadResponse.success(resourceId, R9Values, Type.BOOLEAN);
        case R11:
            return ReadResponse.success(resourceId, this.temperature.isEnable());
        case R12:
            return ReadResponse.success(resourceId, this.temperature.getInterval());
        case R13:
            return ReadResponse.success(resourceId, this.humidity.isEnable());
        case R14:
            return ReadResponse.success(resourceId, this.humidity.getInterval());
        case R15:
            return ReadResponse.success(resourceId, this.pressure.isEnable());
        case R16:
            return ReadResponse.success(resourceId, this.pressure.getInterval());
        case R17:
            return ReadResponse.success(resourceId, this.co2.isEnable());
        case R18:
            return ReadResponse.success(resourceId, this.co2.getInterval());
        case R19:
            return ReadResponse.success(resourceId, this.co.isEnable());
        case R20:
            return ReadResponse.success(resourceId, this.co.getInterval());
        default:
            return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        switch (resourceId) {
            case R7:
                return ExecuteResponse.success();
            case R9:
                return ExecuteResponse.success();
            default:
                return super.execute(identity, resourceId, params);
        }
    }

    @SuppressWarnings("unchecked") 
    @Override
    public synchronized WriteResponse write(ServerIdentity identity, int resourceId, LwM2mResource value) {
        Boolean boolVal = null;
        Integer intVal = null;
        if(!value.isMultiInstances() && GroupSensors.isBoolean(value.getValue().toString())) {
            boolVal = Boolean.parseBoolean(value.getValue().toString());
        }
        if(!value.isMultiInstances() && GroupSensors.isInt(value.getValue().toString())) {
            intVal = Integer.parseInt(value.getValue().toString());
        }
        switch (resourceId) {
        case R9:
            if(value instanceof LwM2mMultipleResource && value.isMultiInstances()) {
                this.R9Values = (Map<Integer, Boolean>)value.getValues();
            } else {
                return WriteResponse.notFound();
            }
        case R11:
            if(boolVal != null) {
                this.temperature.setEnable(boolVal);
                this.temperature.resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R12:
            if(intVal != null) {
                this.temperature.setInterval(intVal);
                this.temperature.resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R13:
            if(boolVal != null) {
                this.humidity.setEnable(boolVal);
                this.humidity.resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R14:
            if(intVal != null) {
                this.humidity.setInterval(intVal);
                this.humidity.resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R15:
            if(boolVal != null) {
                this.pressure.setEnable(boolVal);
                this.pressure.resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R16:
            if(intVal != null) {
                this.pressure.setInterval(intVal);
                this.pressure.resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R17:
            if(boolVal != null) {
                this.co2.setEnable(boolVal);
                this.co2.resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R18:
            if(intVal != null) {
                this.co2.setInterval(intVal);
                this.co2.resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R19:
            if(boolVal != null) {
                this.co.setEnable(boolVal);
                this.co.resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R20:
            if(intVal != null) {
                this.co.setInterval(intVal);
                this.co.resetMeasurementList(new Date());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        default:
            return super.write(identity, resourceId, value);
        }
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
