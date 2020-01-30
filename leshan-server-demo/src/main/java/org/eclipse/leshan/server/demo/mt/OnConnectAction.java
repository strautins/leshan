package org.eclipse.leshan.server.demo.mt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multiset.Entry;
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
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeDeserializer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class OnConnectAction {
    private static final int CFG_HEADER_BYTES = 8;
    private static final int VALUE_BYTES = 4;
    private static final Logger LOG = LoggerFactory.getLogger(OnConnectAction.class);

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private static final String PATH_DEVICES = "/33756/";
    public static final int OBJECT_ID_DEVICES = 33756;

    protected static final int OBJECT_ID_GROUP = 33755;
    private static final String PATH_GROUP = "/33755/";
    private static final String PATH_GROUP_EVENTS = "33755/0/0";
    private static final String PATH_GROUP_EVENT_CFG = "33755/0/1";
    private static final String PATH_GROUP_CLEAR_DATA = "33755/0/2";
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

    /** Sensor object resource ID */
    protected static final int RESOURCE_ID_RESOURCE_MAP = 0;
    protected static final int RESOURCE_ID_FMT = 1;
    protected static final int RESOURCE_ID_CLEAR_MEASUREMENTS = 2;
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
    protected static final int RESOURCE_ID_TEMPERATURE_ALARM = 2;
    protected static final int RESOURCE_ID_CO_ALARM = 3;
    protected static final int RESOURCE_ID_TEMPERATURE = 4;
    protected static final int RESOURCE_ID_CO2 = 5;
    protected static final int RESOURCE_ID_CO = 6;
    protected static final int RESOURCE_ID_HUMIDITY = 7;
    protected static final int RESOURCE_ID_PRESSURE = 8;

    private final LeshanServer mLeshanServer;
    private final ThingsboardSend mThingsboardSend;
    private final SimpleStorage mSimpleStorage;
    private final long mTimeout;

    // enum MeasType : uint8_t {
    //     INT8 = 0,
    //     INT16,
    //     INT32,
    //     FLOAT,
    // };
    private static final Map<Integer, Integer> CFG_BYTES;
    static {
        Map<Integer, Integer> bytes = new HashMap<Integer, Integer>();
        //from server to client params
        bytes.put(0, 1); //1*8=8
        bytes.put(1, 2); //2*8=16
        bytes.put(2, 4); //4*8=32
        bytes.put(3, 4); //4*8=32 
        CFG_BYTES = Collections.unmodifiableMap(bytes);
    }

    private final Gson gson;

    public OnConnectAction(LeshanServer leshanServer, ThingsboardSend thingsboardSend, Pool<Jedis> jedis) throws URISyntaxException {
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

    private final ObservationListener observationListener = new ObservationListener() {
        @Override
        public void newObservation(Observation observation, Registration registration) {
            // LOG.debug("NEW Observation () for {}", observation.getPath().toString(),
            // registration.getEndpoint());
        }

        @Override
        public void cancelled(Observation observation) {
            // LOG.debug("Observation Canceled {}", observation.getPath().toString());
        }

        @Override
        public void onResponse(Observation observation, Registration registration, ObserveResponse response) { 
            if (registration != null && observation.getPath().toString().equals(PATH_GROUP)) {
                LwM2mNode object = readRequest(registration, PATH_ALARM);
                if (object != null && object instanceof LwM2mObject) {
                    int i = mLeshanServer.getObservationService().cancelObservations(registration,
                            observation.getPath().toString());
                }
            }
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            LOG.error("Observation Error {} for {}", observation.getPath().toString(), registration.getEndpoint());
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
            //todo get from memory devices object!
            LwM2mObject devicesObject = null;
            LwM2mObject groupObj = null; 
            LwM2mObject AlarmObj = null; 
            byte[] events = null;
            if(event.equals(EVENT_REGISTRATION)) {
                groupObj = (LwM2mObject)readRequest(registration, PATH_GROUP);
                if(groupObj != null) {
                    events = getOpaque(groupObj.getInstance(0), 0);   
                    putLwM2mObjectInMemory(registration.getEndpoint(), groupObj);  
                }
                devicesObject = (LwM2mObject)readRequest(registration, PATH_DEVICES);
                if(devicesObject != null) {
                    putLwM2mObjectInMemory(registration.getEndpoint(), devicesObject);
                }
            }

            if(events == null) {
                events = readOpaque(registration, PATH_GROUP_EVENTS);
            }
           
            if (events != null) {
                StringBuilder sb = new StringBuilder();
                for(byte bs: events) {
                    sb.append(String.format("%8s", Integer.toBinaryString(bs & 0xFF)).replace(' ', '0')); 
                }
                LOG.error("EVENT LIST: {}", sb.toString());
            } 
            //collect daily sensor data  
            if (isSensors) {
                Payload payload = new Payload();
                int safety = 0; //if device gives misleading info
                while(payload.isRepeatCall() && safety < 3) {
                    safety++;
                    payload.init();
                    processData(registration, PATH_SENSORS, payload);
                    if(payload.isData()) {
                        if(devicesObject == null) {
                            devicesObject = getLwM2mObjectFromMemory(registration.getEndpoint(), OBJECT_ID_DEVICES);
                        }
                        if(devicesObject == null) {
                            devicesObject = (LwM2mObject)readRequest(registration, PATH_DEVICES);
                        }
                        //get device serial info, parse collected data in array
                        if(devicesObject != null) {
                            Map<String, ArrayList<String>> data = new HashMap<String, ArrayList<String>>();
                            for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) devicesObject).getInstances().entrySet()) {
                                String serialNr =  OnConnectAction.getStringResource(entry.getValue(), OnConnectAction.RESOURCE_ID_SERIAL_NUMBER);
                                String p = payload.getPayload(entry.getKey());
                                if(p != null && serialNr != null) {
                                    ArrayList<String> arr = new ArrayList<String>();
                                    arr.add(p);
                                    data.put(serialNr, arr);
                                }
                            }
                            sendAll(data);
                            //clear read data 
                            createExecuteRequest(registration, PATH_GROUP_CLEAR_DATA, null);
                        } else {
                            LOG.error("Could not get serial info from endpoint:{}", registration.getEndpoint());
                            break;
                        }
                    }
                }
            }
            //TODO clear event list on PATH_GROUP_CLEAR_DATA if no sensor data read and event data not empty 
        }
        // collecting request from redis
        // if (this.mSimpleStorage != null) {
        //     processRedisRequests(registration);
        // }
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

    private void observeRequest(Registration registration, String link) {
        try {
            //// leshan client WriteAttributesRequest failed:INTERNAL_SERVER_ERROR not
            //// implemented
            // AttributeSet attributes = AttributeSet.parse("pmin=10&pmax=30");
            // WriteAttributesRequest write = new WriteAttributesRequest(OBJECT_ID_GROUP, attributes);
            // WriteAttributesResponse cResponse = this.mLeshanServer.send(registration,
            //// write, this.mTimeout);
            // if(cResponse.isSuccess()) {
            ReadResponse response = mLeshanServer.send(registration, new ObserveRequest(link), this.mTimeout);
            if (response == null) {
                LOG.debug("ObserveRequest for {} on {} timeout!", registration.getEndpoint(), link);
            } else if (response.isSuccess()) {
                // LOG.debug("ObserveRequest for {} on {} success! : {}",
                // registration.getEndpoint(), link, response.getContent());
            } else {
                LOG.debug("ObserveRequest for {} on {} Failed! Error : {} : {}", registration.getEndpoint(), link,
                        response.getCode(), response.getErrorMessage());
            }
            // } else {
            // }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private LwM2mNode readRequest(Registration registration, String resourceLink) {
        return readRequest(registration, resourceLink, this.mTimeout);
    }

    private LwM2mNode readRequest(Registration registration, String resourceLink, long timeout) {
        LwM2mNode object = null;
        try {
            LOG.debug("Sending read to {} on {} at {}", registration.getEndpoint(), resourceLink,
                    System.currentTimeMillis());
            ReadResponse response = this.mLeshanServer.send(registration, new ReadRequest(resourceLink), timeout);
            // set read values
            if (response == null) {
                LOG.debug("ReadRequest for {} on resource {} timeout! at {}", registration.getEndpoint(), resourceLink,
                        System.currentTimeMillis());
            } else if (response.isSuccess()) {
                object = response.getContent();
                LOG.debug("Received read to {}; on {}; at {}; object {}", registration.getEndpoint(), resourceLink,
                        System.currentTimeMillis(), response.getContent());
            } else {
                LOG.debug("ReadRequest for {} on object {} Failed! Error: {} : {}", registration.getEndpoint(),
                        resourceLink, response.getCode(), response.getErrorMessage());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return object;
    }
    private void multiWriteRequest(Registration registration) {
        Map<Integer, Boolean> values = new HashMap<Integer, Boolean>();
        values.put(0, false);
        values.put(1, false);
        values.put(5, true);
        
        WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, OBJECT_ID_DEVICES, 0,
        LwM2mMultipleResource.newBooleanResource(9, values));
        boolean result = writeRequest(registration, request);
        LOG.debug("MultipleWrite is {}", result);
    }

    private Boolean writeRequest(Registration registration, WriteRequest request) {
        return writeRequest(registration, request, this.mTimeout);
    }

    private Boolean writeRequest(Registration registration, WriteRequest request, long timeout) {
        Boolean result = null;
        String resourceInfo = null;
        if(request.getNode() instanceof LwM2mObjectInstance) {
            resourceInfo = ((LwM2mObjectInstance)request.getNode()).getResources().toString();
        }
        try {
            LOG.debug("Sending write to {} on {}({}) at {}", registration.getEndpoint(), request.getPath().toString(), resourceInfo,
                    System.currentTimeMillis());
            LwM2mResponse response = this.mLeshanServer.send(registration, request, this.mTimeout);
            if (response == null) {
                result = false;
                LOG.debug("WriteRequest for {} on resource {}({}) timeout! as {}", registration.getEndpoint(),
                        request.getPath().toString(), resourceInfo,  System.currentTimeMillis());
            } else if (response.isSuccess()) {
                result = true;
                LOG.debug("Received write to {} on resource {}({}) at {}", registration.getEndpoint(), request.getPath().toString(), resourceInfo,
                        System.currentTimeMillis());
            } else if (!response.isSuccess()) {
                result = false;
                LOG.debug("WriteRequest for {} on resource {}({}) Failed! Error: {} : {}", registration.getEndpoint(),
                        request.getPath().toString(), resourceInfo, response.getCode(), response.getErrorMessage());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
   
    private void processData(Registration registration, String resourceLink, Payload payload) {
        LwM2mNode object = readRequest(registration, resourceLink);
        if (object != null && object instanceof LwM2mObject) {
            LwM2mObject obj = (LwM2mObject) object;
            for (Map.Entry<Integer, LwM2mObjectInstance> entry : obj.getInstances().entrySet()) {
                processDataSub(payload, entry.getValue(), 0);
                processDataSub(payload, entry.getValue(), 1);
                processDataSub(payload, entry.getValue(), 2);
                processDataSub(payload, entry.getValue(), 3);
                processDataSub(payload, entry.getValue(), 4);
            }
        }
    }

    private void processDataSub(Payload payload, LwM2mObjectInstance instance, int resource) {
        byte[] b = getOpaque(instance, resource);
        if(b != null && b.length >= CFG_HEADER_BYTES) { //byte header +? data
            processSensorData(payload, instance.getId(), resource, b); 
        }
    }
    //LITTLE_ENDIAN byte decode
    //4B unixtime, 2B interval, 1B count, 1B cfg, else data
    private void processSensorData(Payload payload,int instance, int resource, byte[] opaque) {
        byte[] rawTime = getEmptyByteArray(0);
        rawTime[0] = opaque[0]; rawTime[1] = opaque[1];
        rawTime[2] = opaque[2]; rawTime[3] = opaque[3];
        byte[] rawInterval = getEmptyByteArray(2);//add last fake bytes for parsing
        rawInterval[0] = opaque[4]; rawInterval[1] = opaque[5];
        int count = opaque[6];
        int unixTime = byteToInt(rawTime, false);
        int interval = byteToInt(rawInterval, false);
        //config as string
        String cfgStr = String.format("%8s", Integer.toBinaryString(opaque[7] & 0xFF)).replace(' ', '0');
        boolean repeatCall = bitStringToInt(cfgStr.substring(6, 7), false) == 1;
        payload.setIfIsRepeatCall(repeatCall);
        int pow = bitStringToInt(cfgStr.substring(3, 6), true); //floating point
        double floatingPoint = Math.pow(10, pow);
        int byteOfValue = CFG_BYTES.get(bitStringToInt(cfgStr.substring(0,3), false)); //value bytes
        double validCount = ((double)(opaque.length - CFG_HEADER_BYTES)) / byteOfValue;

        //->dbg
        StringBuilder sb = new StringBuilder();
        for(byte bs: opaque) {
            sb.append(String.format("%8s", Integer.toBinaryString(bs & 0xFF)).replace(' ', '0')); 
        }
        LOG.error("Config: {}:{}:{}:{}:{}::{}", unixTime, interval, count, cfgStr, validCount, sb.toString());
         //<-dbg
      
        if(opaque.length > CFG_HEADER_BYTES && count == validCount) {
            Map<Integer, Object> collect = new HashMap<Integer, Object>();
            int posInList = 0;
            int offset = VALUE_BYTES - byteOfValue;
            byte[] rawValue = getEmptyByteArray(offset);
            for (int i = 0; i + CFG_HEADER_BYTES < opaque.length; i++) {   
                rawValue[(i % byteOfValue)] = opaque[CFG_HEADER_BYTES + i];
                if(offset + (i % byteOfValue) == VALUE_BYTES - 1) { //last byte in array is added, create value
                    int value = byteToInt(rawValue);
                    int valueTim = unixTime + (posInList * interval);
                    if(pow < 0) {//@floatingPoint must be 0.1,0.01.. //double, round remove 12.2300000000001
                        collect.put(valueTim, getDigitValue((double)value * floatingPoint, Math.abs(pow)));
                    } else {//int @floatingPoint must be 1,10..
                        collect.put(valueTim, value * (int)floatingPoint);
                    }
                    //next item
                    posInList++;
                    rawValue = getEmptyByteArray(offset);
                }
            }
            payload.add(instance, resource, collect);
        }
    }
    //offset fills in LITTLE_ENDIAN last @offset bytes till 4byte array
    private byte[] getEmptyByteArray(int offset) {
        byte[] rawValue = new byte[VALUE_BYTES];//empty array for init
        if(offset > 0) { //if int is 2 byte add 2 last byte for parsing
            for( int i = 0; i < offset; i++) {
                rawValue[(VALUE_BYTES - offset) + i] = 0;
            }
        }
        return rawValue;
    }
    //String with more than 1 byte works only with BIG_ENDIAN  
    public static int bitStringToInt(String b, boolean isSign) {
        int value = 0;
        for (int i = 0; i < b.length(); i++) {
            if(b.charAt(i) == '1') { 
                int add = (int) Math.pow(2, (b.length() - 1 - i));
                if(isSign && i == 0) { //first sing minus
                    add *=-1;
                }
                value += add;
            }
        }
        return value;
    }
    //for unsigned LITTLE_ENDIAN convert to BIG_ENDIAN //todo: improves performance
    public static int byteToInt(byte[] b, boolean isSign) {
        int value = 0;
        StringBuilder sb = new StringBuilder();
        for (byte by : b) {
            sb.insert(0,  String.format("%8s", Integer.toBinaryString(by & 0xFF)).replace(' ', '0')); 
        }
        value = bitStringToInt(sb.toString(), isSign);
        return value;
    }
    //with sign value
    public static int byteToInt(byte[] b) {
        int value = 0;
        for (int i = 0; i < b.length; i++) {
            //value = (value << 8) + (b[i] & 0xff); //BIG_ENDIAN
            value += ((int) b[i] & 0xffL) << (8 * i); //LITTLE_ENDIAN
        }
        return value;
    }

    public static double getDigitValue(double value, int round) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(round, RoundingMode.HALF_UP).doubleValue();
    }

    private void createExecuteRequest(Registration registration, String  path, String parameters) {
            ExecuteRequest request = new ExecuteRequest(path, parameters);
            final String debug = registration.getEndpoint() + "; on" + request.getPath().toString();
            LOG.debug("ExecuteRequest for {} on {} at {}", registration.getEndpoint(),
                    request.getPath().toString(), System.currentTimeMillis());

            this.mLeshanServer.send(registration, request, this.mTimeout, new ResponseCallback<ExecuteResponse>() {
                @Override
                public void onResponse(ExecuteResponse response) {
                    //if(!response.isSuccess()) {
                        LOG.debug("Received Async ExecuteRequest is {} to {} at {} ",  response.isSuccess(), debug, System.currentTimeMillis());
                    //}
                }
            }, new ErrorCallback() {
                @Override
                public void onError(Exception e) {
                    LOG.error("onError Async ExecuteRequest on {} : {}", debug, e.getMessage());
                }
            });
    }
    // private Boolean clearInstance(Registration registration, Integer objectId, LwM2mObjectInstance inst) {
    //     Boolean result = false;
    //     Date lmt = getDateResource(inst, RESOURCE_ID_LMT);
    //     if (lmt != null) {
    //         WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, objectId, inst.getId(),
    //                 LwM2mSingleResource.newDateResource(RESOURCE_ID_LRMT, lmt));
    //         result = writeRequest(registration, request);
    //     } else {
    //         LOG.debug("Clear object skipped for {} on object {}/{} because last measurement time is null",
    //         registration.getEndpoint(), objectId, inst.getId());
    //     }
    //     return result;
    // }
    // private void clearObjectAsync(Registration registration, LwM2mObject object) {
    //     final CountDownLatch latch = new CountDownLatch(object.getInstances().size());
    //     for (Map.Entry<Integer, LwM2mObjectInstance> entry : object.getInstances().entrySet()) {
    //         Date lmt = getDateResource(entry.getValue(), RESOURCE_ID_LMT);
    //         if (lmt != null) {
    //             WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, object.getId(), entry.getKey(),
    //                     LwM2mSingleResource.newDateResource(RESOURCE_ID_LRMT, lmt));
    //             final String debug = registration.getEndpoint() + ":" + object.getId() + " / " + request.getPath().toString();
    //             this.mLeshanServer.send(registration, request, this.mTimeout, new ResponseCallback<WriteResponse>() {
    //                 @Override
    //                 public void onResponse(WriteResponse response) {
    //                     latch.countDown();
    //                     LOG.debug("onResponse  {} : {} ", response.isSuccess(), debug);
    //                 }
    //             }, new ErrorCallback() {
    //                 @Override
    //                 public void onError(Exception e) {
    //                     LOG.debug("onError {} : {}", debug, e.getMessage());
    //                 }
    //             });
    //         } else {
    //             latch.countDown();
    //             LOG.debug("Clear object skipped for {} on object {}/{} because last measurement time is null",
    //                     registration.getEndpoint(), object.getId(), entry.getKey());
    //         }

    //     }
    //     LOG.debug("latch.await for {} : {}", registration.getEndpoint(), object.getId());
    //     try {
    //         Boolean result = latch.await(this.mTimeout, TimeUnit.MILLISECONDS);
    //         if(!result) {
    //             LOG.debug("latch.await ended with timeout for {} : {}", registration.getEndpoint(), object.getId());     
    //         }
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }
    //     LOG.debug("latch.await ended for {} : {}", registration.getEndpoint(), object.getId());
    // }
    private byte[] readOpaque(Registration registration, String resourceLink) {
        byte[] value = null;
        LwM2mNode object = readRequest(registration, resourceLink);
        if (object != null && object instanceof LwM2mSingleResource) {
            LwM2mSingleResource res = (LwM2mSingleResource) object;
            if (res.getType().equals(Type.OPAQUE)) {
                value = (byte[])res.getValue();
            } else {
                LOG.error("Unknown  ({}) resource type {}; expected OPAQUE. EP: {}", resourceLink, res.getType(),
                        registration.getEndpoint());
            }
        }
        return value;
    }

    private Boolean readBooleanResource(Registration registration, String resourceLink) {
        Boolean value = null;
        LwM2mNode object = readRequest(registration, resourceLink);
        if (object != null && object instanceof LwM2mSingleResource) {
            LwM2mSingleResource res = (LwM2mSingleResource) object;
            if (res.getType().equals(Type.BOOLEAN)) {
                value = (boolean) res.getValue();
            } else {
                LOG.error("Unknown  ({}) resource type {}; expected BOOLEAN. EP: {}", resourceLink, res.getType(),
                        registration.getEndpoint());
            }
        }
        return value;
    }

    static protected  Boolean getBooleanResource(LwM2mObjectInstance instObj, int res) {
        Boolean result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.BOOLEAN)) {
                result = (Boolean) lmtRes.getValue();
            }
        }
        return result;
    }
    static protected String getStringResource(LwM2mObjectInstance instObj, int res) {
        String result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.STRING)) {
                result = (String) lmtRes.getValue();
            }
        }
        return result;
    }

    static protected Date getDateResource(LwM2mObjectInstance instObj, int res) {
        Date result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.TIME)) {
                result = (Date) lmtRes.getValue();
            }
        }
        return result;
    }

    static protected Long getIntegerResource(LwM2mObjectInstance instObj, int res) {
        Long result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.INTEGER)) {
                result = (Long) lmtRes.getValue();
            }
        }
        return result;
    }
    static protected Double getDoubleResource(LwM2mObjectInstance instObj, int res) {
        Double result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.FLOAT)) {
                result = (Double) lmtRes.getValue();
            }
        }
        return result;
    }

    static protected Map<Integer, Object> getResourceMap(LwM2mObjectInstance instObj, int res) {
        LwM2mResource resource = instObj.getResources().get(res);
        if(resource != null && resource instanceof LwM2mMultipleResource && resource.isMultiInstances()) {
            @SuppressWarnings("unchecked")
            Map<Integer, Object> resourceMap = (Map<Integer, Object>) ((LwM2mMultipleResource)resource).getValues();
            return resourceMap;
        } else {
            LOG.error("Error on getResourceMap. resource is {} null; resource is {} instance of LwM2mMultipleResource; resource is {} MultiInstances; Resource: {}",
                (resource != null ? "not" : ""), (resource instanceof LwM2mMultipleResource ? "" : "not"), (resource != null && resource.isMultiInstances() ? "": "not"), res);
        }
        return null;
    }

    static protected  byte[] getOpaque(LwM2mObjectInstance instObj, int res) {
        LwM2mResource resource = instObj.getResources().get(res);
        if(resource != null && resource instanceof LwM2mSingleResource) {
            @SuppressWarnings("unchecked")
            byte[] resourceMap = (byte[])((LwM2mSingleResource)resource).getValue();
            return resourceMap;
        } else {
            LOG.error("Error on getOpaque. {}", resource);
        }
        return null;
    }
    //================================================================================
    // payloads send
    //================================================================================
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