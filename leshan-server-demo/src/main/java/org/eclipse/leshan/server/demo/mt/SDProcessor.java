package org.eclipse.leshan.server.demo.mt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.interceptors.MessageInterceptorAdapter;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.Link;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.mt.tb.ThingsboardSend;
import org.eclipse.leshan.server.demo.mt.memory.InMemoryStorage;
import org.eclipse.leshan.server.demo.mt.memory.RedisStorage;
import org.eclipse.leshan.server.demo.mt.memory.SimpleStorage;
import org.eclipse.leshan.server.demo.mt.tb.Payload;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeDeserializer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.observation.ObservationListener;
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

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class SDProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SDProcessor.class);

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private static final String PATH_DEVICES = "/33756/";
    public static final int OBJECT_ID_DEVICES = 33756;

    protected static final int OBJECT_ID_GROUP = 33755;
    private static final String PATH_GROUP = "/33755/";
    private static final LwM2mPath PATH_GROUP_EVENTS = new LwM2mPath("33755/0/0");
    private static final LwM2mPath PATH_GROUP_EVENT_CFG = new LwM2mPath("33755/0/1");
    private static final LwM2mPath PATH_GROUP_CLEAR_DATA = new LwM2mPath("33755/0/2");
    /** Sensor object temperature */
    private static final String PATH_SENSORS = "/33758/";
    /** Alarm object path */
    private static final String PATH_ALARM = "/33757/";
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

    protected static final String NAME_ALARM_TEMPERATURE = "alarm_temperature";
    protected static final String NAME_ALARM_HUMIDITY = "alarm_humidity";
    protected static final String NAME_ALARM_CO2 = "alarm_co2";
    protected static final String NAME_ALARM_CO = "alarm_co";
    protected static final String NAME_ALARM_ATMOSPHERIC = "alarm_atmospheric";

    /** Alarm object resource ID */
    protected static final int RESOURCE_ID_SMOKE_ALARM = 0;
    protected static final int RESOURCE_ID_HUSHED = 1;
    protected static final int RESOURCE_ID_CO_ALARM = 2;
    protected static final int RESOURCE_ID_TEMPERATURE = 3;
    protected static final int RESOURCE_ID_HUMIDITY = 4;
    protected static final int RESOURCE_ID_PRESSURE = 5;
    protected static final int RESOURCE_ID_CO2 = 6;
    protected static final int RESOURCE_ID_CO = 7;

    private final LeshanServer mLeshanServer;
    private final ThingsboardSend mThingsboardSend;
    private final SimpleStorage mSimpleStorage;
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

    public SDProcessor(LeshanServer leshanServer, ThingsboardSend thingsboardSend, Pool<Jedis> jedis) throws URISyntaxException {
        this.mLeshanServer = leshanServer;
        this.mSimpleStorage = jedis != null ? new RedisStorage(jedis) : new InMemoryStorage();
        this.mThingsboardSend = thingsboardSend;
        this.mTimeout = 10000;
        
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeDeserializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }

    private final MessageInterceptorAdapter messageInterceptorAdapter = new MessageInterceptorAdapter() {
        @Override
        public void sendRequest(final Request request) {
            request.setNanoTimestamp(System.currentTimeMillis());
            request.addMessageObserver(new MessageObserver() {
                @Override
                public void onTimeout() {
                    LOG.debug("timeout at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onSendError(Throwable error) {
                    LOG.debug("sent error {} at {} : {}", error, System.currentTimeMillis(), request);
                }

                @Override
                public void onRetransmission() {
                    LOG.debug("retransmission at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onResponse(Response response) {
                    LOG.debug("get response {} at {} : {}", response, System.currentTimeMillis(), request);
                }

                @Override
                public void onReject() {
                    LOG.debug("rejected at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onReadyToSend() {
                    LOG.debug("ready to send at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onDtlsRetransmission(int flight) {
                    LOG.debug("retransmit flight {}  at {} : {}", flight, System.currentTimeMillis(), request);
                }

                @Override
                public void onContextEstablished(EndpointContext endpointContext) {
                    LOG.debug("context establiched at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onConnecting() {
                    LOG.debug("connecting at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onComplete() {
                    LOG.debug("completed at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onCancel() {
                    LOG.debug("cancelled at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onAcknowledgement() {
                    LOG.debug("acknowledged at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onSent(boolean retransmission) {
                    LOG.debug("sent at {} : {} : {}", System.currentTimeMillis(), request, retransmission);
                }
            });
        };
    };

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

    public void start() {
        if(this.mThingsboardSend != null) {
            this.mThingsboardSend.start();
        }
        this.mLeshanServer.getRegistrationService().addListener(this.registrationListener);
        // this.mLeshanServer.getObservationService().addListener(this.observationListener);
        // this.mLeshanServer.coap().getUnsecuredEndpoint().addInterceptor(this.messageInterceptorAdapter);

        // MyCoapMessageTracer coapMessageTracer = new MyCoapMessageTracer();
        // for (Endpoint endpoint :
        // this.mLeshanServer.coap().getServer().getEndpoints()) {
        // endpoint.addInterceptor(coapMessageTracer);
        // }
    }

    public void stop() {
        this.mThingsboardSend.stop();
        this.mLeshanServer.getRegistrationService().removeListener(this.registrationListener);
        // this.mLeshanServer.getObservationService().removeListener(this.observationListener);
        // this.mLeshanServer.coap().getUnsecuredEndpoint().removeInterceptor(this.messageInterceptorAdapter);
        // for (Endpoint endpoint :
        // this.mLeshanServer.coap().getServer().getEndpoints()) {
        // endpoint.removeInterceptor(coapMessageTracer);
        // }
    }

    private void wrapperGetResources(Registration registration, String event) {
        if (event.equals(EVENT_REGISTRATION) || event.equals(EVENT_UPDATED)) {          
            if(registration.getObjectLinks() != null) {
                getResources(registration, event);
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
            if (i.getUrl().contains(PATH_GROUP)) {
                isMainObj = true;
            } else if (i.getUrl().contains(PATH_SENSORS)) {
                isSensors = true;
            } else if (i.getUrl().contains(PATH_ALARM)) {
                isAlarm = true;
            } else if (i.getUrl().contains(PATH_DEVICES)) {
                isDevices = true;
            }
        }
        // testing
        // if (isDevices) {
        //     ResourceModel resourceModel = this.mLeshanServer.getModelProvider().getObjectModel(registration)
        //         .getObjectModel(OBJECT_ID_DEVICES).resources.get(9);
        //     multiWriteRequest(registration);
        // }
        if (isMainObj && isDevices) {
            
            LwM2mObject devicesObject = null;
            LwM2mObject groupObj = null; 
            LwM2mObject AlarmObj = null; 
            boolean needToClearData = false;
            byte[] events = null;
            if(event.equals(EVENT_REGISTRATION)) {
                groupObj = (LwM2mObject)Lwm2mHelper.readRequest(this.mLeshanServer, registration, PATH_GROUP, this.mTimeout);
                if(groupObj != null) {
                    putLwM2mObjectInMemory(registration.getEndpoint(), groupObj);  
                    events = Lwm2mHelper.getOpaque(groupObj.getInstance(PATH_GROUP_EVENTS.getObjectInstanceId()), PATH_GROUP_EVENTS.getResourceId());   
                    byte[] cfg = Lwm2mHelper.getOpaque(groupObj.getInstance(PATH_GROUP_EVENT_CFG.getObjectInstanceId()), PATH_GROUP_EVENT_CFG.getResourceId());   
                    cfg = getEndpointCfg(registration, cfg);//get byte cfg
                    Lwm2mHelper.writeOpaque(this.mLeshanServer, registration, PATH_GROUP_EVENT_CFG, cfg, this.mTimeout);//write byte cfg
                }

                devicesObject = (LwM2mObject)Lwm2mHelper.readRequest(this.mLeshanServer, registration, PATH_DEVICES, this.mTimeout);
                if(devicesObject != null) {
                    putLwM2mObjectInMemory(registration.getEndpoint(), devicesObject);
                    Map<Integer, byte[]> cfg = getEndpointOutputCfg(registration);
                    for(Map.Entry<Integer, LwM2mObjectInstance> entry : devicesObject.getInstances().entrySet()) {
                        Lwm2mHelper.writeOpaque(this.mLeshanServer, registration, new LwM2mPath(devicesObject.getId(),entry.getKey(), 10), cfg, this.mTimeout);
                    }
                }
            } else if(events == null) {
                events = Lwm2mHelper.readOpaque(this.mLeshanServer, registration, PATH_GROUP_EVENTS, this.mTimeout);
            }
           
            if (events != null) {
                needToClearData = true;
                //do something wise on received events
                for(byte[] b: ByteUtil.split(events, 4)) {
                    PredefinedEvent ev = new PredefinedEvent(b);
                    LOG.info("EVENT: {}", ev.toString());
                    if(ev.getEventCode().equals(EventCode.ALARM)) {
                        AlarmObj = (LwM2mObject)Lwm2mHelper.readRequest(this.mLeshanServer, registration, PATH_ALARM, this.mTimeout);     
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
                    processData(this.mLeshanServer, registration, PATH_SENSORS, payload, this.mTimeout);
                    if(payload.isData()) {
                        if(devicesObject == null) {
                            devicesObject = getLwM2mObjectFromMemory(registration.getEndpoint(), OBJECT_ID_DEVICES);
                        }
                        if(devicesObject == null) {
                            devicesObject = (LwM2mObject)Lwm2mHelper.readRequest(this.mLeshanServer, registration, PATH_DEVICES, this.mTimeout);
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
                            Lwm2mHelper.createExecuteRequest(this.mLeshanServer, registration, PATH_GROUP_CLEAR_DATA.toString(), null, this.mTimeout);
                        } else {
                            LOG.error("Could not get serial info from endpoint:{}", registration.getEndpoint());
                            break;
                        }
                    }
                }
            }

            //clear read event data
            if(needToClearData) {
                Lwm2mHelper.createExecuteRequest(this.mLeshanServer, registration, PATH_GROUP_CLEAR_DATA.toString(), null, this.mTimeout);  
            }
        }
        // collecting request from redis
        // if (this.mSimpleStorage != null) {
        //     processRedisRequests(registration);
        // }
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
        LOG.info(c.toString());
        c =  new OutputStateConfig(OutputPolarity.LOW, EventCode.ALARM, 1l, OutputTriggerType.EQUAL_OR_GREATER);
        LOG.info(c.toString());
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
                processDataSub(payload, entry.getValue(), 0); //temperateure
                processDataSub(payload, entry.getValue(), 1); //humidity
                processDataSub(payload, entry.getValue(), 2); //Preasure
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
    private void processSensorData(Payload payload,int instance, int resource, byte[] opaque) {
        byte[] rawTime = ByteUtil.getEmptyByteArray(0);
        rawTime[0] = opaque[0]; rawTime[1] = opaque[1];
        rawTime[2] = opaque[2]; rawTime[3] = opaque[3];
        byte[] rawInterval = ByteUtil.getEmptyByteArray(2);//add last fake bytes for parsing
        rawInterval[0] = opaque[4]; rawInterval[1] = opaque[5];
        int count = opaque[6];
        int unixTime = ByteUtil.byteToInt(rawTime, false);
        int interval = ByteUtil.byteToInt(rawInterval, false);
        //config as string
        String cfgStr = String.format("%8s", Integer.toBinaryString(opaque[7] & 0xFF)).replace(' ', '0');
        boolean repeatCall = ByteUtil.bitStringToInt(cfgStr.substring(6, 7), false) == 1;
        payload.setIfIsRepeatCall(repeatCall);
        int pow = ByteUtil.bitStringToInt(cfgStr.substring(3, 6), true); //floating point
        double floatingPoint = Math.pow(10, pow);
        int byteOfValue = CFG_BYTES.get(ByteUtil.bitStringToInt(cfgStr.substring(0,3), false)); //value bytes
        double validCount = ((double)(opaque.length - ByteUtil.CFG_HEADER_BYTES)) / byteOfValue;

        //->dbg
        StringBuilder sb = new StringBuilder();
        for(byte bs: opaque) {
            sb.append(ByteUtil.byteToString(bs)); 
        }
        LOG.debug("Config: {}:{}:{}:{}:{}::{}", unixTime, interval, count, cfgStr, validCount, sb.toString());
         //<-dbg
      
        if(opaque.length > ByteUtil.CFG_HEADER_BYTES && count == validCount) {
            Map<Integer, Object> collect = new HashMap<Integer, Object>();
            int posInList = 0;
            int offset = ByteUtil.VALUE_BYTES - byteOfValue;
            byte[] rawValue = ByteUtil.getEmptyByteArray(offset);
            for (int i = 0; i + ByteUtil.CFG_HEADER_BYTES < opaque.length; i++) {   
                rawValue[(i % byteOfValue)] = opaque[ByteUtil.CFG_HEADER_BYTES + i];
                if(offset + (i % byteOfValue) == ByteUtil.VALUE_BYTES - 1) { //last byte in array is added, create value
                    //int value = ByteBuffer.wrap(rawValue).order(ByteOrder.LITTLE_ENDIAN).getInt(); 
                    int value = ByteUtil.byteToInt(rawValue);
                    int valueTim = unixTime + (posInList * interval);
                    if(pow < 0) {//@floatingPoint must be 0.1,0.01.. //double, round remove 12.2300000000001
                        collect.put(valueTim, ByteUtil.getDoubleRound((double)value * floatingPoint, Math.abs(pow)));
                    } else {//int @floatingPoint must be 1,10..
                        collect.put(valueTim, value * (int)floatingPoint);
                    }
                    //next item
                    posInList++;
                    rawValue = ByteUtil.getEmptyByteArray(offset);
                }
            }
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

    //================================================================================
    // For Redis instructions!
    //================================================================================
    // private void processRedisRequests(Registration registration) {
    //     Map<String, String> requestPayload = this.mRedisStorage.getEndpointRequests(registration.getEndpoint());
    //     if (requestPayload != null) {
    //         for (Map.Entry<String, String> entry : requestPayload.entrySet()) {
    //             RedisRequestLink requestLink = new RedisRequestLink(entry.getKey(), entry.getValue());
    //             if (!requestLink.isError()) {
    //                 sendRequest(registration, requestLink);
    //             }
    //             entry.setValue(requestLink.getResponse());
    //         }
    //         this.mRedisStorage.sendResponse(registration.getEndpoint(), requestPayload);
    //     }
    // }

    // private void sendRequest(Registration registration, RedisRequestLink objLink) {
    //     try {
    //         if (objLink.isRead()) {
    //             ReadResponse response = this.mLeshanServer.send(registration, new ReadRequest(objLink.getLink()),
    //                     this.mTimeout);
    //             // set read values
    //             if (response == null) {
    //                 JSONObject res = new JSONObject();
    //                 res.put("error", "timeout");
    //                 objLink.setResponse(res);
    //             } else if (response.isSuccess()) {
    //                 objLink.setResponse(OnConnectAction.this.gson.toJson(response.getContent()));
    //             } else {
    //                 JSONObject res = new JSONObject();
    //                 res.put("error", response.getErrorMessage());
    //                 res.put("code", response.getCode());
    //                 objLink.setResponse(res);
    //             }
    //         } else if (objLink.isWrite()) {
    //             ResourceModel resourceModel = this.mLeshanServer.getModelProvider().getObjectModel(registration)
    //                     .getObjectModel(objLink.getObjectId()).resources.get(objLink.getResourceId());
    //                     String version = this.mLeshanServer.getModelProvider().getObjectModel(registration)
    //                     .getObjectModel(objLink.getObjectId()).getVersion();
    //             if (resourceModel != null) {
    //                 try {
    //                     Object value = null;
    //                     if (resourceModel.type.equals(org.eclipse.leshan.core.model.ResourceModel.Type.BOOLEAN)) {
    //                         value = Boolean.valueOf(objLink.getValue().toString());
    //                     } else if (resourceModel.type
    //                             .equals(org.eclipse.leshan.core.model.ResourceModel.Type.INTEGER)) {
    //                         value = Long.valueOf(objLink.getValue().toString());
    //                     } else if (resourceModel.type.equals(org.eclipse.leshan.core.model.ResourceModel.Type.STRING)) {
    //                         value = String.valueOf(objLink.getValue().toString());
    //                     }
    //                     if (value != null) {
    //                         WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, objLink.getObjectId(),
    //                                 objLink.getInstanceId(), LwM2mSingleResource.newResource(objLink.getResourceId(),
    //                                         value, resourceModel.type));
    //                         // blocking request
    //                         WriteResponse response = this.mLeshanServer.send(registration, request, this.mTimeout);
    //                         if (response == null) {
    //                             JSONObject res = new JSONObject();
    //                             res.put("error", "timeout");
    //                             objLink.setResponse(res);
    //                         } else if (response.isSuccess()) {
    //                             JSONObject res = new JSONObject();
    //                             res.put("result", response.getCoapResponse().toString());
    //                             objLink.setResponse(res);
    //                         } else {
    //                             JSONObject res = new JSONObject();
    //                             res.put("error", response.getErrorMessage());
    //                             res.put("code", response.getCode());
    //                             objLink.setResponse(res);
    //                         }
    //                     } else {
    //                         JSONObject res = new JSONObject();
    //                         res.put("error", "Resource type not implemented! " + resourceModel.type);
    //                         objLink.setResponse(res);
    //                     }
    //                 } catch (Exception e) {
    //                     JSONObject res = new JSONObject();
    //                     res.put("error", "Write value cast error: " + objLink.getValue());
    //                     objLink.setResponse(res);
    //                 }
    //             } else {
    //                 JSONObject res = new JSONObject();
    //                 res.put("error", "Resource model not found! " + objLink.getObjectId() + objLink.getResourceId());
    //                 objLink.setResponse(res);
    //             }
    //         } else {
    //             JSONObject res = new JSONObject();
    //             res.put("error", "Action not implemented!");
    //             objLink.setResponse(res);
    //         }
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }
    // }
}