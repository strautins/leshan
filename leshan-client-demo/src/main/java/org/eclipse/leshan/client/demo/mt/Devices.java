package org.eclipse.leshan.client.demo.mt;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.mikrotik.iot.sd.utils.ByteUtil;
import org.mikrotik.iot.sd.utils.OutputStateConfig;
import org.mikrotik.iot.sd.utils.PredefinedEvent;
import org.mikrotik.iot.sd.utils.CodeWrapper.EventCode;
import org.mikrotik.iot.sd.utils.CodeWrapper.OutputPolarity;
import org.mikrotik.iot.sd.utils.CodeWrapper.OutputTriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;

public class Devices extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(Devices.class);
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

    private static final List<Integer> supportedResources = Arrays.asList(R0, R1, R2, R3, R4, R5, R6, R7, R8, R9,
        R10, R11, R12, R13, R14, R15, R16, R17, R18, R19);
    private final ScheduledExecutorService scheduler;

    private String R0Value = "1.2.0";
    private String R1Value = "1.1.0r2";
    private String R2Value= "";
    private Boolean R3Value= true;
    /**Last Active Time */
    private Date R4Value;
    private long R5Value;
    private long R6Value = 0l;
    private Map<Integer, Double> R7Value = new HashMap<Integer, Double>();
    private Map<Integer, Boolean> R9Values = new HashMap<Integer, Boolean>();
    private Map<Integer, OutputStateConfig> R10Values = new HashMap<Integer, OutputStateConfig>();
    private Boolean R18Value = true;
    private Boolean R19Value = true;

    private SensorReadings mSensor = null;

    private GroupSensors mGroupSensors;
    
    private final Random mRnd = new Random();

    public Devices() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Devices"));
        initDefault();
        R7Value.put(0, ByteUtil.getDoubleRound(1 + mRnd.nextDouble(), 3)); //Battery Level 
        R7Value.put(1, ByteUtil.getDoubleRound(1 + mRnd.nextDouble(), 3));
        R9Values.put(0, true); //Output State
        R9Values.put(1, false);
        R10Values.put(0, new OutputStateConfig(OutputPolarity.HIGH, EventCode.ALARM, 1l, OutputTriggerType.EQUAL_OR_GREATER));
        R10Values.put(1, new OutputStateConfig(OutputPolarity.LOW, EventCode.ALARM, 1l, OutputTriggerType.EQUAL_OR_GREATER));

        scheduleEvent();
    }

    public void initDefault() {
        R3Value = true; //Reachable
        R4Value = new Date(); //Last Active Time
        R5Value = -60l + (long)(-10 * mRnd.nextDouble()); //Bluetooth Signal Strength
        fireResourcesChange(R4, R5);
        //R9Values.put(0, true); //Output State
        //R9Values.put(1, false);
    }

    public void setGroupSensors(GroupSensors groupSensors) {
        this.mGroupSensors = groupSensors;
    }

    private void scheduleEvent() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                scheduleEvent();  
                initDefault();
                double d = mRnd.nextDouble();
                if(d < 0.3) {
                    PredefinedEvent ev = new PredefinedEvent(EventCode.NO_EVENT);
                    ev.addInstance(getId());
                    if(d < 0.1) {
                        ev.setEventCode(EventCode.BATTERY_PROB);
                        if(d < 0.05) {
                            R7Value.put(0, 0d);
                        } else {
                            R7Value.put(1, 0d);
                        }
                    } else if(d < 0.1) {
                        ev.setEventCode(EventCode.CRIT_BAT);
                        if(R6Value != 2) {
                            R6Value = 2;
                            R7Value.put(0, ByteUtil.getDoubleRound(mRnd.nextDouble(), 3));
                            R7Value.put(1, ByteUtil.getDoubleRound(mRnd.nextDouble(), 3));
                        } else {
                            R6Value = 0;
                            R7Value.put(0, ByteUtil.getDoubleRound(0.5 + mRnd.nextDouble(), 3));
                            R7Value.put(1, ByteUtil.getDoubleRound(0.5 + mRnd.nextDouble(), 3));   
                        }
                    } else if(d < 0.15) {
                        ev.setEventCode(EventCode.NOT_REACHABLE);
                        R3Value = false;
                        R4Value = new Date(System.currentTimeMillis() - (long)(mRnd.nextDouble() * 60000d));
                    } else if(d < 0.2) {
                        ev.setEventCode(EventCode.EXT_PWR);
                        if(R19Value) {
                            R19Value = false;
                        } else {
                            R19Value = true;
                        }
                    }  else if(d < 0.25) {
                        R5Value = -100 + (long)(-20 * mRnd.nextDouble());
                        ev.setEventCode(EventCode.LOW_BT_SIGNAL);
                    } else {
                        ev.setEventCode(EventCode.DAMPER);
                        if(R18Value) {
                            R18Value = false;
                        } else {
                            R18Value = true;
                        }
                    }
                    LOG.info("Creating Device EVENT:{};", ev.toString());
                    mGroupSensors.pushEvents(ev); 
                }
            }
        }, 90 + mRnd.nextInt(90), TimeUnit.SECONDS);
    }

    public void setSerialNumber(String serialNr) {
        this.R2Value = serialNr;      
    }

    public void setSensorReadings(SensorReadings value) {
        this.mSensor = value;      
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
            return ReadResponse.success(resourceId, R7Value, Type.FLOAT);
        case R9:
            return ReadResponse.success(resourceId, R9Values, Type.BOOLEAN);
        case R10:
            Map<Integer, byte[]> tmpArr = new HashMap<Integer, byte[]>(); 
            for(Map.Entry<Integer, OutputStateConfig> entry : this.R10Values.entrySet()) {
                tmpArr.put(entry.getKey(), entry.getValue().toWriteByte());
            }
            return ReadResponse.success(resourceId, tmpArr, Type.OPAQUE);
        case R12:
            return ReadResponse.success(resourceId, this.mSensor.getSensor(SensorReadings.R0).getInterval());
        case R13:
            return ReadResponse.success(resourceId, this.mSensor.getSensor(SensorReadings.R1).getInterval());
        case R14:
            return ReadResponse.success(resourceId, this.mSensor.getSensor(SensorReadings.R2).getInterval());
        case R15:
            return ReadResponse.success(resourceId, this.mSensor.getSensor(SensorReadings.R3).getInterval());
        case R16:
            return ReadResponse.success(resourceId, this.mSensor.getSensor(SensorReadings.R4).getInterval());
        case R17:
            return ReadResponse.success(resourceId, this.mSensor.getSensor(SensorReadings.R5).getInterval());
        case R18:
            return ReadResponse.success(resourceId, this.R18Value);
        case R19:
            return ReadResponse.success(resourceId, this.R19Value);
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
        Integer intVal = null;
        if(!value.isMultiInstances() && value.getType().equals(ResourceModel.Type.INTEGER)) {
            intVal = Integer.parseInt(value.getValue().toString());
        }
        switch (resourceId) {
        case R9:
            if(value instanceof LwM2mMultipleResource && value.isMultiInstances()) {
                this.R9Values = (Map<Integer, Boolean>)value.getValues();
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            }
        case R10:
            if(value instanceof LwM2mMultipleResource 
                    && value.isMultiInstances() && value.getType().equals(ResourceModel.Type.OPAQUE)) {
                    Map<Integer, byte[]> tmpArr = (Map<Integer, byte[]>)value.getValues();
                Map<Integer, OutputStateConfig> tmpList = new HashMap<Integer, OutputStateConfig>();
                for(Map.Entry<Integer, byte[]> entry : tmpArr.entrySet()) {
                    OutputStateConfig ops = new OutputStateConfig(entry.getValue());
                    if(ops.isValid()) {
                        tmpList.put(entry.getKey(), ops);
                    } else {
                        //do nothing?      
                    }
                }

                if(tmpList.size() == tmpArr.size()) {
                    this.R10Values.clear();
                    this.R10Values.putAll(tmpList);
                    
                    for(Map.Entry<Integer, OutputStateConfig> entry : this.R10Values.entrySet()) {
                        LOG.info("OutputStateConfig for instance {}; Config{}", getId(), entry.getValue().toString());
                    }
                    return WriteResponse.success();
                } else {
                    return WriteResponse.badRequest("Corrupted configuration");    
                }
            } else {
                return WriteResponse.badRequest("Not multiple, Not OPAQUE");
            }
        case R12:
            if(intVal != null) {
                this.mSensor.getSensor(SensorReadings.R0).setInterval(intVal);
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R13:
            if(intVal != null) {
                this.mSensor.getSensor(SensorReadings.R1).setInterval(intVal);
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R14:
            if(intVal != null) {
                this.mSensor.getSensor(SensorReadings.R2).setInterval(intVal);
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R15:
            if(intVal != null) {
                this.mSensor.getSensor(SensorReadings.R3).setInterval(intVal);
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R16:
            if(intVal != null) {
                this.mSensor.getSensor(SensorReadings.R4).setInterval(intVal);
                return WriteResponse.success();
            } else {
                return WriteResponse.notFound();
            } 
        case R17:
            if(intVal != null) {
                this.mSensor.getSensor(SensorReadings.R5).setInterval(intVal);
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
