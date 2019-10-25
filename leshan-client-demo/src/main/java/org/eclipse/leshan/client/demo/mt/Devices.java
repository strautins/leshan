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

    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2, R3, R4, R5, R6, R7, R8, R9);
    private final ScheduledExecutorService scheduler;
    private Integer mInterval = 10;

    private String R0Value = "1.2.0";
    private String R1Value = "1.1.0r2";
    private String R2Value= "";
    private Boolean R3Value= true;
    /**Last Active Time */
    private Date R4Value = new Date();
    private long R5Value = 0l;
    private double R6Value = 1.2321;
    private Boolean R8Value = false;

    public Devices() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Devices"));
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
        case R8:
            return ReadResponse.success(resourceId, R8Value);
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

    @Override
    public synchronized WriteResponse write(ServerIdentity identity, int resourceid, LwM2mResource value) {
        switch (resourceid) {
        case R8:
            if(GroupSensors.isBoolean(value.getValue().toString())) {
                this.R8Value = Boolean.parseBoolean(value.getValue().toString());
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            }     
        default:
            return super.write(identity, resourceid, value);
        }
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
