package org.eclipse.leshan.server.demo.mt;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.leshan.Link;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.mt.tb.ThingsboardSend;
import org.eclipse.leshan.server.demo.mt.scheduler.ScheduleRequest;
import org.eclipse.leshan.server.demo.mt.scheduler.ScheduleRequestDeserializer;
import org.eclipse.leshan.server.demo.mt.scheduler.ScheduleRequestSerializer;
import org.eclipse.leshan.server.demo.mt.scheduler.ScheduleRequest.ActionType;
import org.eclipse.leshan.server.demo.mt.memory.SimpleStorage;
import org.eclipse.leshan.server.demo.mt.tb.Payload;
import org.eclipse.leshan.server.demo.servlet.EventServlet;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeDeserializer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.mikrotik.iot.sd.utils.ByteUtil;
import org.mikrotik.iot.sd.utils.CodeWrapper;
import org.mikrotik.iot.sd.utils.CustomEvent;
import org.mikrotik.iot.sd.utils.OutputStateConfig;
import org.mikrotik.iot.sd.utils.PredefinedEvent;
import org.mikrotik.iot.sd.utils.CodeWrapper.EventCode;
import org.mikrotik.iot.sd.utils.CodeWrapper.OutputPolarity;
import org.mikrotik.iot.sd.utils.CodeWrapper.OutputTriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SDProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SDProcessor.class);

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private static final LwM2mPath PATH_DEVICES = new LwM2mPath("/33756/");
    private static final LwM2mPath PATH_GROUP = new LwM2mPath("/33755/");
    private static final LwM2mPath PATH_SENSORS = new LwM2mPath("/33758/");
    private static final LwM2mPath PATH_ALARM = new LwM2mPath("/33757/"); 

    private static final LwM2mPath PATH_GROUP_EVENTS = new LwM2mPath("33755/0/0");
    private static final LwM2mPath PATH_GROUP_EVENT_CFG = new LwM2mPath("33755/0/1");
    private static final LwM2mPath PATH_GROUP_CLEAR_DATA = new LwM2mPath("33755/0/2");
    /** Devices resource names */
    protected static final String NAME_BATTERY = "battery";
    protected static final String NAME_BATTERY_LEVEL = "battery_level";
    protected static final String NAME_LATEST_TIME = "latest_time";
    protected static final String NAME_REACHABLE = "reachable";
    /** Devices resource ID */
    protected static final int RESOURCE_ID_SERIAL_NUMBER = 2;
    protected static final int RESOURCE_ID_REACHABLE = 3;
    protected static final int RESOURCE_ID_LAST_ACTIVE_TIME = 4;
    protected static final int RESOURCE_ID_BLUETOOTH_SIGNAL = 5;
    protected static final int RESOURCE_ID_BATTERY = 6;
    protected static final int RESOURCE_ID_BATTERY_LEVEL = 7;

    /** Alarm resource names */
    protected static final String NAME_SMOKE_ALARM = "smoke_alarm";
    protected static final String NAME_HUSHED = "hushed";
    protected static final String NAME_TEMPERATURE_ALARM = "temperature_alarm";
    protected static final String NAME_CO_ALARM = "co_alarm";

    /** Alarm object resource ID */
    protected static final int RESOURCE_ID_SMOKE_ALARM = 0;
    protected static final int RESOURCE_ID_CO_ALARM = 1;
    protected static final int RESOURCE_ID_TEMPERATURE = 2;
    protected static final int RESOURCE_ID_HUSHED = 3;

    private final LeshanServer mLeshanServer;
    private final ThingsboardSend mThingsboardSend;
    private final SimpleStorage mSimpleStorage;
    // to push scheduled Read to interace
    private EventServlet mEventServlet = null; 

    private final long mTimeout;

    private static final Map<Integer, Integer> CFG_BYTES;
    static {
        Map<Integer, Integer> bytes = new HashMap<Integer, Integer>();
        bytes.put(0, 1); //1*8=8
        bytes.put(1, 2); //2*8=16
        bytes.put(2, 4); //4*8=32
        bytes.put(3, 4); //4*8=32 
        CFG_BYTES = Collections.unmodifiableMap(bytes);
    }

    private final Gson gson;

    public SDProcessor(LeshanServer leshanServer, ThingsboardSend thingsboardSend, SimpleStorage simpleStorage) throws URISyntaxException {
        this.mLeshanServer = leshanServer;
        this.mSimpleStorage = simpleStorage;
        this.mThingsboardSend = thingsboardSend;
        this.mTimeout = 10000;
        
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeDeserializer());
        gsonBuilder.registerTypeHierarchyAdapter(ScheduleRequest.class, new ScheduleRequestSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(ScheduleRequest.class, new ScheduleRequestDeserializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }

    private final RegistrationListener registrationListener = new RegistrationListener() {

        @Override
        public void registered(Registration registration, Registration previousReg,
                Collection<Observation> previousObservations) {
            wrapperGetResources(registration, EVENT_REGISTRATION);
        }

        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                Registration previousRegistration) {
            wrapperGetResources(updatedRegistration, EVENT_UPDATED);
        }

        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                Registration newReg) {
            wrapperGetResources(registration, EVENT_DEREGISTRATION);
        }
    };

    public void setEventServlet(EventServlet event) {
        this.mEventServlet = event;     
    }

    public void start() {
        if(this.mThingsboardSend != null) {
            this.mThingsboardSend.start();
        }
        this.mLeshanServer.getRegistrationService().addListener(this.registrationListener);
    }

    public void stop() {
        if(this.mThingsboardSend != null) {
            this.mThingsboardSend.stop();
        }
        this.mLeshanServer.getRegistrationService().removeListener(this.registrationListener);
    }

    private void wrapperGetResources(Registration registration, String event) {
        if (event.equals(EVENT_REGISTRATION) || event.equals(EVENT_UPDATED)) {          
            if(registration.getObjectLinks() != null) {
                long start = System.currentTimeMillis();
                getResources(registration, event);
                LOG.info("Process of Endpoint {} took {} ms", registration.getEndpoint(), (System.currentTimeMillis() - start));
            }
        }
    }

    private void getResources(Registration registration, String event) {
        // todo check observations
        // Set<Observation> observ = mLeshanServer.getObservationService().getObservations(registration);
        // for (Observation s : observ) {
        //     LOG.debug("Observations for {} / {} : {}", registration.getEndpoint(), event, s.toString());
        // }
        boolean isMainObj = false;
        boolean isDevices = false;
        boolean isSensors = false;
        boolean isAlarm = false;
        for (Link i : registration.getObjectLinks()) {
            if (i.getUrl().contains(PATH_GROUP.toString())) {
                isMainObj = true;
            } else if (i.getUrl().contains(PATH_SENSORS.toString())) {
                isSensors = true;
            } else if (i.getUrl().contains(PATH_ALARM.toString())) {
                isAlarm = true;
            } else if (i.getUrl().contains(PATH_DEVICES.toString())) {
                isDevices = true;
            }
        }

        if (isMainObj && isDevices) {
            LwM2mObject devicesObject = null;
            LwM2mObject groupObj = null; 
            LwM2mObject AlarmObj = null; 
            boolean needToClearData = false;
            byte[] events = null;
            if(event.equals(EVENT_REGISTRATION)) {
                groupObj = (LwM2mObject)Lwm2mHelper.readRequest(this.mLeshanServer, registration, PATH_GROUP.toString(), this.mTimeout);
                if(groupObj != null) {
                    putLwM2mObjectInMemory(registration.getEndpoint(), groupObj);  
                    events = Lwm2mHelper.getOpaque(groupObj.getInstance(PATH_GROUP_EVENTS.getObjectInstanceId()), PATH_GROUP_EVENTS.getResourceId());   
                    byte[] cfg = Lwm2mHelper.getOpaque(groupObj.getInstance(PATH_GROUP_EVENT_CFG.getObjectInstanceId()), PATH_GROUP_EVENT_CFG.getResourceId());   
                    cfg = getEndpointCfg(registration, cfg);//get byte cfg
                    Lwm2mHelper.writeOpaque(this.mLeshanServer, registration, PATH_GROUP_EVENT_CFG, cfg, this.mTimeout);//write byte cfg
                }

                devicesObject = (LwM2mObject)Lwm2mHelper.readRequest(this.mLeshanServer, registration, PATH_DEVICES.toString(), this.mTimeout);
                if(devicesObject != null) {
                    putLwM2mObjectInMemory(registration.getEndpoint(), devicesObject);
                    Map<Integer, byte[]> cfg = getEndpointOutputCfg(registration);
                    for(Map.Entry<Integer, LwM2mObjectInstance> entry : devicesObject.getInstances().entrySet()) {
                        Lwm2mHelper.writeOpaque(this.mLeshanServer, registration, new LwM2mPath(devicesObject.getId(),entry.getKey(), 10), cfg, this.mTimeout);
                    }
                }
            } else {
                events = Lwm2mHelper.readOpaque(this.mLeshanServer, registration, PATH_GROUP_EVENTS, this.mTimeout);
            }
           
            if (events != null) {
                //do something wise on received events
                for(byte[] b: ByteUtil.split(events, 4)) {
                    PredefinedEvent ev = new PredefinedEvent(b);
                    LOG.info("EVENT: {}", ev.toString());
                    if(!ev.getEventCode().equals(EventCode.NO_EVENT)) {
                        needToClearData = true;
                    }
                    if(ev.getEventCode().equals(EventCode.ALARM) && isAlarm) {
                        AlarmObj = (LwM2mObject)Lwm2mHelper.readRequest(this.mLeshanServer, registration, PATH_ALARM.toString(), this.mTimeout);     
                        if(AlarmObj != null) {
                            putLwM2mObjectInMemory(registration.getEndpoint(), AlarmObj); 
                        }
                    }
                }
            } 

            //collect daily sensor data  
            if (isSensors) {
                Payload payload = new Payload();
                int safety = 0; //if device gives misleading info
                while(payload.isRepeatCall() && safety < 3) {
                    safety++;
                    payload.init();
                    processData(this.mLeshanServer, registration, PATH_SENSORS.toString(), payload, this.mTimeout);
                    if(payload.isData()) {
                        if(devicesObject == null) {
                            devicesObject = getLwM2mObjectFromMemory(registration.getEndpoint(), PATH_DEVICES.getObjectId());
                        }
                        if(devicesObject == null) {
                            devicesObject = (LwM2mObject)Lwm2mHelper.readRequest(this.mLeshanServer, registration, PATH_DEVICES.toString(), this.mTimeout);
                        }
                        //get device serial info, parse collected data in array
                        if(devicesObject != null) {
                            Map<String, ArrayList<String>> data = new HashMap<String, ArrayList<String>>();
                            for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) devicesObject).getInstances().entrySet()) {
                                String serialNr =  Lwm2mHelper.getStringResource(entry.getValue(), SDProcessor.RESOURCE_ID_SERIAL_NUMBER);
                                String p = payload.getPayload(entry.getKey());
                                if(p != null && serialNr != null) {
                                    ArrayList<String> arr = new ArrayList<String>();
                                    arr.add(p);
                                    data.put(serialNr, arr);
                                }
                            }
                            sendAll(data);
                            //clear read data 
                            needToClearData = false;
                            Lwm2mHelper.asyncExecuteRequest(this.mLeshanServer, registration, PATH_GROUP_CLEAR_DATA.toString(), null, this.mTimeout);
                        } else {
                            LOG.error("Could not get serial info from endpoint:{}", registration.getEndpoint());
                            break;
                        }
                    }
                }
            }

            //clear read event data
            if(needToClearData) {
                Lwm2mHelper.asyncExecuteRequest(this.mLeshanServer, registration, PATH_GROUP_CLEAR_DATA.toString(), null, this.mTimeout);  
            }
        }
        //check for scheduled calls
        processScheduledOperations(registration);
    }

    public byte[] getEndpointCfg(Registration registration,  byte[] currentByteCfg) {
        CustomEvent ev1 = new CustomEvent(CodeWrapper.EventCode.TEMP_EVENT,
            CustomEvent.EventTriggerType.UP, true, 20.34f, 1, 2);
        CustomEvent ev2 = new CustomEvent(CodeWrapper.EventCode.TEMP_EVENT,
            CustomEvent.EventTriggerType.BOTH, false, 10.88f, 0, 2, 4);
        CustomEvent ev3 = new CustomEvent(CodeWrapper.EventCode.TEMP_EVENT,
            CustomEvent.EventTriggerType.DOWN, true, -5, 0, 1);
        return ByteUtil.concatenate(ev1.toWriteByte(), ev2.toWriteByte(), ev3.toWriteByte());
    }

    public Map<Integer, byte[]> getEndpointOutputCfg(Registration registration) {
        Map<Integer, byte[]> output = new HashMap<Integer, byte[]>();
        OutputStateConfig c = new OutputStateConfig(OutputPolarity.HIGH, EventCode.ALARM, 1l, OutputTriggerType.EQUAL_OR_GREATER);
        output.put(0, c.toWriteByte());
        c =  new OutputStateConfig(OutputPolarity.LOW, EventCode.ALARM, 1l, OutputTriggerType.EQUAL_OR_GREATER);
        output.put(1, c.toWriteByte());
        
        return output;
    }

    private void putLwM2mObjectInMemory(String endpoint, LwM2mObject obj) {
        if(this.mSimpleStorage != null) {
            String response = this.gson.toJson(obj);
            this.mSimpleStorage.setResource(endpoint, "/" + obj.getId() +  "/", response);
        }
    }

    private LwM2mObject getLwM2mObjectFromMemory(String endpoint, int id) {
        LwM2mObject obj = null;
        if(this.mSimpleStorage != null) {
            String response = this.mSimpleStorage.getResource(endpoint, "/" + id + "/");
            if(response != null) {
                obj = this.gson.fromJson(response, LwM2mObject.class);
            }
        }
        return obj;
    }

    private void processData(LeshanServer server, Registration registration, String resourceLink, Payload payload, long timeout) {
        LwM2mNode object = Lwm2mHelper.readRequest(server, registration, resourceLink, timeout);
        if (object != null && object instanceof LwM2mObject) {
            LwM2mObject obj = (LwM2mObject) object;
            for (Map.Entry<Integer, LwM2mObjectInstance> entry : obj.getInstances().entrySet()) {
                processDataSub(payload, entry.getValue(), 0); //temperature
                processDataSub(payload, entry.getValue(), 1); //humidity
                processDataSub(payload, entry.getValue(), 2); //Pressure
                processDataSub(payload, entry.getValue(), 3); //CO2
                processDataSub(payload, entry.getValue(), 4); //CO
                processDataSub(payload, entry.getValue(), 5); //IAQ
            }
        }
    }

    private void processDataSub(Payload payload, LwM2mObjectInstance instance, int resource) {
        byte[] b = Lwm2mHelper.getOpaque(instance, resource);
        if(b != null && b.length >= ByteUtil.CFG_HEADER_BYTES) { //byte header +? data
            processSensorData(payload, instance.getId(), resource, b); 
        }
    }
    //LITTLE_ENDIAN byte decode //use ByteBuffer??
    //4B unixtime, 2B interval, 1B count, 1B cfg, else data
    private void processSensorData(Payload payload, int instance, int resource, byte[] opaque) {
        //->dbg
        // StringBuilder sb = new StringBuilder();
        // for(byte bs: opaque) {
        //     sb.append(ByteUtil.byteToString(bs)); 
        //     sb.append(" "); 
        // }
        //LOG.debug("Instance:{};Resource:{};Opaque.length:{};Byte[]:{};",instance,resource, opaque.length, sb.toString());
        int posStart = 0;
        Map<Integer, Object> collect = null;
        int maxPacketCount = 5; //safety, do we need?
        int currentPacket = 0; 
        while (posStart != opaque.length && currentPacket < maxPacketCount) {
            currentPacket++;
            byte[] byteArray = ByteUtil.getEmptyByteArray(0);
            byteArray[0] = opaque[posStart + 0]; byteArray[1] = opaque[posStart + 1];
            byteArray[2] = opaque[posStart + 2]; byteArray[3] = opaque[posStart + 3];
            int unixTime = ByteUtil.byteToInt(byteArray, false);

            byteArray[0] = opaque[posStart + 4]; byteArray[1] = opaque[posStart + 5];
            byteArray[2] = 0; byteArray[3] = 0;
            int interval = ByteUtil.byteToInt(byteArray, false);
            int valueCount = opaque[posStart + 6];
            //config as string
            String cfgStr = ByteUtil.byteToString(opaque[posStart + 7]);
            boolean repeatCall = ByteUtil.bitStringToInt(cfgStr.substring(6, 7), false) == 1;
            payload.setIfIsRepeatCall(repeatCall);
            int pow = ByteUtil.bitStringToInt(cfgStr.substring(3, 6), true); //floating point
            double powValue = Math.pow(10, pow);
            int byteOfValue = CFG_BYTES.get(ByteUtil.bitStringToInt(cfgStr.substring(0,3), false)); //value bytes

            int packetByteSize = ByteUtil.CFG_HEADER_BYTES + valueCount * byteOfValue;
            // LOG.debug("HEADER: {} : {} : {} : {} : {} : {} : {}",posStart, unixTime, interval, valueCount, pow, byteOfValue, packetByteSize);
            if(opaque.length >= posStart + packetByteSize && valueCount > 0) {
                if(collect == null) {
                    collect = new HashMap<Integer, Object>();
                }
                int posInList = 0;
                int offset = ByteUtil.VALUE_BYTES - byteOfValue;
                byteArray[0] = 0; byteArray[1] = 0;byteArray[2] = 0; byteArray[3] = 0;
                for (int i = 0; i  < (valueCount * byteOfValue); i++) {   
                    int payloadPos = posStart + ByteUtil.CFG_HEADER_BYTES + i;
                    int valuePos = (i % byteOfValue);
                    byteArray[valuePos] = opaque[payloadPos];
                    if(offset + valuePos == ByteUtil.VALUE_BYTES - 1) { //last byte in value array is added, create value
                        //int value = ByteBuffer.wrap(rawValue).order(ByteOrder.LITTLE_ENDIAN).getInt(); 
                        int value = ByteUtil.byteToInt(byteArray);
                        int valueTim = unixTime + (posInList * interval);
                        if(pow < 0) {//@floatingPoint must be 0.1,0.01.. //double, round remove 12.2300000000001
                            collect.put(valueTim, ByteUtil.getDoubleRound((double)value * powValue, Math.abs(pow)));
                        } else {//int @floatingPoint must be 1,10..
                            collect.put(valueTim, value * (int)powValue);
                        }
                        posInList++;
                    }
                }
                posStart += packetByteSize;
            } else { //if something wrong with packet or only header provided
                posStart = opaque.length;
            }
        }

        if(collect != null) {
            payload.add(instance, resource, collect);
        }
    }

    private void sendAll(Map<String, ArrayList<String>> data) {
        if (this.mThingsboardSend != null) {
            for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
                if(entry.getValue().size() > 0) {
                    this.mThingsboardSend.send(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void processScheduledOperations(Registration registration) {
        Map<String, String> requestPayload = this.mSimpleStorage.getEndpointRequests(registration.getEndpoint());
        if (requestPayload != null) {
            for (Map.Entry<String, String> entry : requestPayload.entrySet()) {
                ScheduleRequest request;
                try {
                   request = this.gson.fromJson(entry.getValue(), ScheduleRequest.class);    
                   if (request.isValid()) {
                        boolean result = sendRequest(registration, request);
                        LOG.info("Process scheduled request for {} of {} is {} : {}", registration.getEndpoint(), entry.getKey(), result, request.getReadResponse());
                        if(result && this.mEventServlet != null) {
                            if(result && request.isRead()) {
                                mEventServlet.pushEvent(registration, request);
                            } else if(request.isWrite()) {
                                request.setActionType(ActionType.read);
                                result = sendRequest(registration, request); 
                                if(result) {
                                    mEventServlet.pushEvent(registration, request);
                                }   
                            } 
                        }
                    } 
                } catch(Exception e) {
                    LOG.error("Error processing scheduled request {} for {}:{}", entry.getKey(), registration.getEndpoint(), entry.getValue());
                }
                this.mSimpleStorage.deleteEndpointRequest(registration.getEndpoint(), entry.getKey());
            }
        }
    }

    private boolean sendRequest(Registration registration, ScheduleRequest request) {
        try {
            if (request.isRead()) {
                LwM2mResponse obj = Lwm2mHelper.send(this.mLeshanServer, registration, request.getReadRequest(), this.mTimeout);
                if(obj != null && obj instanceof ReadResponse) {
                    LwM2mNode node = ((ReadResponse)obj).getContent();
                    request.setReadResponse(node);
                    return true;
                }
            } else if (request.isWrite()) {
                LwM2mResponse obj = Lwm2mHelper.send(this.mLeshanServer, registration, request.getWriteRequest(), this.mTimeout);
                return obj != null && obj.isSuccess();
            } else if (request.isExecute()) {
                LwM2mResponse obj = Lwm2mHelper.send(this.mLeshanServer, registration, request.getExecuteRequest(), this.mTimeout);
                return obj != null && obj.isSuccess();
            } else if (request.isObserve()) {
                LwM2mResponse obj = Lwm2mHelper.send(this.mLeshanServer, registration, request.getObserveRequest(), this.mTimeout);
                return obj != null && obj.isSuccess();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}