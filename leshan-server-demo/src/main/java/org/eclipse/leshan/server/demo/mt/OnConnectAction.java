package org.eclipse.leshan.server.demo.mt;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnConnectAction {

    private static final Logger LOG = LoggerFactory.getLogger(OnConnectAction.class);

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";
    //redis json serial mapping key
    private static final String JSON_MAPPING_KEY = "mapping";
    //??
    private static final String JSON_DEVICES_KEY = "devices";

    private static final String PATH_DEVICES = "/43001/";

    private static final int OBJECT_ID_GROUP = 43000;
    private static final String PATH_GROUP = "/43000/";
    private static final String PATH_GROUP_ATTENTION_REQUIRED = "43000/0/0";
    private static final String PATH_GROUP_ALARM_TRIGGERED = "43000/0/1";
    private static final String PATH_GROUP_DEVICES_INFO_CHANGED = "43000/0/2";
    /**Sensor object temperature */
    private static final int OBJECT_ID_TEMPERATURE = 43002;
    private static final String PATH_TEMPERATURE = "/43002/";
    private static final String NAME_TEMPERATURE = "temperature";
    /**Sensor object humidity */
    private static final int OBJECT_ID_HUMIDITY = 43003;
    private static final String PATH_HUMIDITY = "/43003/";
    private static final String NAME_HUMIDITY = "humidity";
    /**Sensor object co2 */
    private static final int OBJECT_ID_CO2 = 43006;
    private static final String PATH_CO2 = "/43006/";
    private static final String NAME_CO2 = "co2";
    /**Sensor object co */
    private static final int OBJECT_ID_CO = 43007;
    private static final String PATH_CO = "/43007/";
    private static final String NAME_CO = "co";
    /**Sensor object atmospheric */
    private static final int OBJECT_ID_ATMOSPHERIC = 43004;
    private static final String PATH_ATMOSPHERIC = "/43004/";
    private static final String NAME_ATMOSPHERIC = "atmospheric";
    /**Alarm object path */
    private static final String PATH_ALARM = "/43005/";
    /** Devices resource names */
    private static final String NAME_BATTERY = "battery";
    private static final String NAME_BATTERY_LEVEL = "battery_level";
    private static final String NAME_LATEST_TIME = "latest_time";
    private static final String NAME_REACHABLE = "reachable";
    /** Devices resource ID */
    private static final int RESOURCE_ID_SERIAL_NUMBER = 2;
    private static final int RESOURCE_ID_REACHABLE = 3;
    private static final int RESOURCE_ID_LAST_ACTIVE_TIME = 4;
    private static final int RESOURCE_ID_BATTERY = 5;
    private static final int RESOURCE_ID_BATTERY_LEVEL = 6;
    /** Sensor object resource ID */
    private static final int RESOURCE_ID_PULSE = 1;
    private static final int RESOURCE_ID_RESOURCE_MAP = 2;
    private static final int RESOURCE_ID_LMT = 3;
    private static final int RESOURCE_ID_LRMT = 4;
    /**Alarm resource names */
    private static final String NAME_SMOKE_ALARM = "smoke_alarm";
    private static final String NAME_HUSHED = "hushed";
    private static final String NAME_TEMPERATURE_ALARM = "temperature_alarm";
    private static final String NAME_CO_ALARM = "co_alarm";
    
    private static final String NAME_ALARM_TEMPERATURE = "alarm_temperature";
    private static final String NAME_ALARM_HUMIDITY = "alarm_humidity";
    private static final String NAME_ALARM_CO2 = "alarm_co2";
    private static final String NAME_ALARM_CO = "alarm_co";
    private static final String NAME_ALARM_ATMOSPHERIC = "alarm_atmospheric";
    /** Alarm object resource ID */
    private static final int RESOURCE_ID_SMOKE_ALARM = 0;
    private static final int RESOURCE_ID_HUSHED = 1;
    private static final int RESOURCE_ID_TEMPERATURE_ALARM = 2;
    private static final int RESOURCE_ID_CO_ALARM = 3;
    private static final int RESOURCE_ID_TEMPERATURE = 4;
    private static final int RESOURCE_ID_CO2 = 5;
    private static final int RESOURCE_ID_CO = 6;
    private static final int RESOURCE_ID_HUMIDITY = 7;
    private static final int RESOURCE_ID_PRESSURE = 8;

    /** Group resource ID  */
    private static final int RESOURCE_ID_ALARM_TRIGGERED = 1;
    private static final int RESOURCE_ID_DEVICES_INFO_CHANGED = 2;
    /** dummy resource name */
    private static final String NAME_NONE = "none";

    private final LeshanServer mLeshanServer;
    private final ThingsboardMqttClient mThingsboardMqttClient;
    private final ThingsboardHttpClient mThingsboardHttpClient;
    private final RedisMessage mRedisMessage;
    private final long mTimeout;

    private final Gson gson;

    public OnConnectAction(LeshanServer leshanServer, ThingsboardMqttClient lwM2mMqttClient,
            ThingsboardHttpClient thingsboardHttpClient, RedisMessage redisMessage) throws URISyntaxException {
        this.mLeshanServer = leshanServer;
        this.mThingsboardMqttClient = lwM2mMqttClient;
        this.mRedisMessage = redisMessage;
        this.mThingsboardHttpClient = thingsboardHttpClient;
        this.mTimeout = 5000;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }

    private final MessageInterceptorAdapter messageInterceptorAdapter = new MessageInterceptorAdapter() {
        @Override
        public void sendRequest(final Request request) {
            request.setTimestamp(System.currentTimeMillis());
            request.addMessageObserver(new MessageObserver() {
                @Override
                public void onTimeout() {
                    LOG.warn("timeout at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onSent() {
                    LOG.warn("sent at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onSendError(Throwable error) {
                    LOG.warn("sent error {} at {} : {}", error, System.currentTimeMillis(), request);
                }

                @Override
                public void onRetransmission() {
                    LOG.warn("retransmission at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onResponse(Response response) {
                    LOG.warn("get response {} at {} : {}", response, System.currentTimeMillis(), request);
                }

                @Override
                public void onReject() {
                    LOG.warn("rejected at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onReadyToSend() {
                    LOG.warn("ready to send at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onDtlsRetransmission(int flight) {
                    LOG.warn("retransmit flight {}  at {} : {}", flight, System.currentTimeMillis(), request);
                }

                @Override
                public void onContextEstablished(EndpointContext endpointContext) {
                    LOG.warn("context establiched at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onConnecting() {
                    LOG.warn("connecting at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onComplete() {
                    LOG.warn("completed at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onCancel() {
                    LOG.warn("cancelled at {} : {}", System.currentTimeMillis(), request);
                }

                @Override
                public void onAcknowledgement() {
                    LOG.warn("acknowledged at {} : {}", System.currentTimeMillis(), request);
                }
            });
        };
    };

    private final RegistrationListener registrationListener = new RegistrationListener() {

        @Override
        public void registered(Registration registration, Registration previousReg,
                Collection<Observation> previousObservations) {
            getResources(registration, EVENT_REGISTRATION);
        }

        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                Registration previousRegistration) {
            getResources(updatedRegistration, EVENT_UPDATED);
        }

        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                Registration newReg) {
            getResources(registration, EVENT_DEREGISTRATION);
        }

    };

    private final ObservationListener observationListener = new ObservationListener() {
        @Override
        public void newObservation(Observation observation, Registration registration) {
            // LOG.warn("NEW Observation () for {}", observation.getPath().toString(),
            // registration.getEndpoint());
        }

        @Override
        public void cancelled(Observation observation) {
            // LOG.warn("Observation Canceled {}", observation.getPath().toString());
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
            LOG.warn("Observation Error {} for {}", observation.getPath().toString(), registration.getEndpoint());
        }
    };

    public void start() {
        this.mLeshanServer.getRegistrationService().addListener(this.registrationListener);
        this.mLeshanServer.getObservationService().addListener(this.observationListener);
        // this.mLeshanServer.coap().getUnsecuredEndpoint().addInterceptor(this.messageInterceptorAdapter);

        // MyCoapMessageTracer coapMessageTracer = new MyCoapMessageTracer();
        // for (Endpoint endpoint :
        // this.mLeshanServer.coap().getServer().getEndpoints()) {
        // endpoint.addInterceptor(coapMessageTracer);
        // }
    }

    public void stop() {
        this.mLeshanServer.getRegistrationService().removeListener(this.registrationListener);
        this.mLeshanServer.getObservationService().removeListener(this.observationListener);
        // this.mLeshanServer.coap().getUnsecuredEndpoint().removeInterceptor(this.messageInterceptorAdapter);
        // for (Endpoint endpoint :
        // this.mLeshanServer.coap().getServer().getEndpoints()) {
        // endpoint.removeInterceptor(coapMessageTracer);
        // }
    }
    private String getSensorName(Integer objectId) {
        String result = null;
        switch(objectId) {
            case OBJECT_ID_TEMPERATURE:
                result = NAME_TEMPERATURE;
                break;
            case OBJECT_ID_HUMIDITY:
                result = NAME_HUMIDITY;
                break;
            case OBJECT_ID_ATMOSPHERIC:
                result = NAME_ATMOSPHERIC;
                break;
             case OBJECT_ID_CO:
                result = NAME_CO;
                break;
            case OBJECT_ID_CO2:
                result = NAME_CO2;
                break;
            default:
                result = NAME_NONE;
        }      
        return result;
    }

    private void getResources(Registration registration, String event) {
        LOG.warn("Online {} with {}", registration.getEndpoint(), event);
        // todo check observations
        Set<Observation> observ = mLeshanServer.getObservationService().getObservations(registration);
        for (Observation s : observ) {
            LOG.warn("Observations for {} / {} : {}", registration.getEndpoint(), event, s.toString());
        }

        if (event.equals(EVENT_REGISTRATION) || event.equals(EVENT_UPDATED)) {
            if (registration.getObjectLinks() != null) {
                boolean isMainObj = false;
                boolean isHumidity = false;
                boolean isCo2 = false;
                boolean isTemperature = false;
                boolean isCo = false;
                boolean isAtmospheric = false;
                boolean isAlertObject = false;
                for (Link i : registration.getObjectLinks()) {
                    if (i.getUrl().contains(PATH_GROUP)) {
                        isMainObj = true;
                    } else if (i.getUrl().contains(PATH_TEMPERATURE)) {
                        isTemperature = true;
                    } else if (i.getUrl().contains(PATH_HUMIDITY)) {
                        isHumidity = true;
                    } else if (i.getUrl().contains(PATH_CO2)) {
                        isCo2 = true;
                    } else if (i.getUrl().contains(PATH_CO)) {
                        isCo = true;
                    } else if (i.getUrl().contains(PATH_ATMOSPHERIC)) {
                        isAtmospheric = true;
                    } else if (i.getUrl().contains(PATH_ALARM)) {
                        isAlertObject = true;
                    }
                }
                if (isMainObj) {
                    Boolean isAttentionRequired = readBooleanResource(registration, PATH_GROUP_ATTENTION_REQUIRED);
                    if (isAttentionRequired == null) {
                        // error occurred while getting flag from device;
                    } else {
                        //collection for payloads to thingsboard!
                        Map<String, Map<Long, JSONObject>> payloads = new HashMap<String, Map<Long, JSONObject>>();
                        JSONObject endpointInfo = null;
                        Map<Integer, String> serialMapping = null;
                        if(mRedisMessage != null) {
                            endpointInfo = getRedisEndpointInfo(registration.getEndpoint());    
                            serialMapping = getFromJsonSerialMapping(endpointInfo);
                        }
                        //if attention required or serial ,mapping is missing
                        if (isAttentionRequired || serialMapping == null) {
                            Boolean isAlarmTriggered = readBooleanResource(registration, PATH_GROUP_ALARM_TRIGGERED);
                            Boolean isDevicesInfoChanged = readBooleanResource(registration, PATH_GROUP_DEVICES_INFO_CHANGED);
                            if (isDevicesInfoChanged != null && isDevicesInfoChanged || serialMapping == null) {
                                //clear flag aware of changes
                                if(isDevicesInfoChanged) {
                                    WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, OBJECT_ID_GROUP, 0,
                                        LwM2mSingleResource.newBooleanResource(RESOURCE_ID_DEVICES_INFO_CHANGED, false));
                                    writeRequest(registration, request);
                                }
                                //read devices object, get mapping collect payloads for battery, reachability, last activity time
                                LwM2mNode devicesObject = readRequest(registration, PATH_DEVICES);
                                serialMapping = getSerialMapping(registration, devicesObject);
                                setDevicesPayload(registration, devicesObject, payloads);
                                //if exists redis db, add to json and tush to redis
                                if(mRedisMessage != null) {
                                    //mapping
                                    endpointInfo = addToJsonSerialMapping(endpointInfo, serialMapping);
                                    //endpointInfo = addToJsonDevicesObject(endpointInfo, devicesObject);
                                    setRedisEndpointInfo(registration.getEndpoint(), endpointInfo);
                                }
                            }
                            
                            if (isAlarmTriggered != null && isAlarmTriggered && isAlertObject) {
                                if(isAlarmTriggered) {
                                    WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, OBJECT_ID_GROUP, 0,
                                        LwM2mSingleResource.newBooleanResource(RESOURCE_ID_ALARM_TRIGGERED, false));
                                    writeRequest(registration, request);
                                }
                                //todo maybe observe all object check changes!
                                LwM2mNode alarmObject = readRequest(registration, PATH_ALARM);
                                setAlarmPayload(registration, alarmObject, serialMapping, payloads);
                            }
                        }

                        if (serialMapping != null) {

                            if (isTemperature) {
                                processData(registration, serialMapping, PATH_TEMPERATURE, payloads);
                            }
                            if (isHumidity) {
                                processData(registration, serialMapping, PATH_HUMIDITY, payloads);
                            }
                            if (isCo2) {
                                processData(registration, serialMapping, PATH_CO2, payloads);
                            }
                            if (isCo) {
                                processData(registration, serialMapping, PATH_CO, payloads);
                            }
                            if (isAtmospheric) {
                                processData(registration, serialMapping, PATH_ATMOSPHERIC, payloads);
                            }
                            // collecting request from redis
                            if (this.mRedisMessage != null) {
                                processRedisRequests(registration, serialMapping);
                            }
                        }
                        
                        Map<String, ArrayList<String>> result = serializePayloadAll(payloads);
                        sendAll(result);
                    }
                }
            }
        }
    }

    private Boolean readBooleanResource(Registration registration, String resourceLink) {
        Boolean value = null;
        LwM2mNode object = readRequest(registration, resourceLink);
        if (object != null && object instanceof LwM2mSingleResource) {
            LwM2mSingleResource res = (LwM2mSingleResource) object;
            if (res.getType().equals(Type.BOOLEAN)) {
                value = (boolean) res.getValue();
            } else {
                LOG.warn("Unknown  ({}) resource type {}; expected BOOLEAN. EP: {}", resourceLink, res.getType(),
                        registration.getEndpoint());
            }
        }
        return value;
    }

    private void observeRequest(Registration registration, String link) {
        try {
            //// leshan client WriteAttributesRequest failed:INTERNAL_SERVER_ERROR not
            //// implemented
            // AttributeSet attributes = AttributeSet.parse("pmin=10&pmax=30");
            // WriteAttributesRequest write = new WriteAttributesRequest(43000, attributes);
            // WriteAttributesResponse cResponse = this.mLeshanServer.send(registration,
            //// write, this.mTimeout);
            // if(cResponse.isSuccess()) {
            ReadResponse response = mLeshanServer.send(registration, new ObserveRequest(link), this.mTimeout);
            if (response == null) {
                LOG.warn("ObserveRequest for {} on {} timeout!", registration.getEndpoint(), link);
            } else if (response.isSuccess()) {
                // LOG.warn("ObserveRequest for {} on {} success! : {}",
                // registration.getEndpoint(), link, response.getContent());
            } else {
                LOG.warn("ObserveRequest for {} on {} Failed! Error : {} : {}", registration.getEndpoint(), link,
                        response.getCode(), response.getErrorMessage());
            }
            // } else {
            // }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendAll(Map<String, ArrayList<String>> data) {
        if (this.mThingsboardHttpClient != null) {
            for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
                if(entry.getValue().size() > 0) {
                    send2HttpApi(entry.getValue(), entry.getKey());
                }
            }
        }
        if (this.mThingsboardMqttClient != null) {
            for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
                if(entry.getValue().size() > 0) {
                    send2Mqtt(entry.getValue(), entry.getKey());
                }
            }
        }
        // if (this.mRedisMessage != null) {
        // this.mRedisMessage.sendPayload(data);
        // for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
        // this.mRedisMessage.writeEventList(entry.getKey());
        // }
        // }
    }

    private void send2Mqtt(ArrayList<String> payloadArray, String token) {
        try {
            this.mThingsboardMqttClient.connectAndPublish(token, payloadArray);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void send2HttpApi(ArrayList<String> payloadArray, String token) {
        try {
            this.mThingsboardHttpClient.post2ThingsBoard(token, payloadArray);
        } catch (IOException e) {
            e.printStackTrace();
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
            ReadResponse response = this.mLeshanServer.send(registration, new ReadRequest(resourceLink), timeout);
            // set read values
            if (response == null) {
                LOG.warn("ReadRequest for {} on resource {} timeout!", registration.getEndpoint(), resourceLink);
            } else if (response.isSuccess()) {
                object = response.getContent();
            } else {
                LOG.warn("ReadRequest for {} on object {} Failed! Error: {} : {}", registration.getEndpoint(),
                        resourceLink, response.getCode(), response.getErrorMessage());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return object;
    }
    private Boolean writeRequest(Registration registration, WriteRequest request) {
        return writeRequest(registration, request, this.mTimeout);
    }
    private Boolean writeRequest(Registration registration, WriteRequest request, long timeout) {
        Boolean result = null;
        try {
            LwM2mResponse response = this.mLeshanServer.send(registration, request, this.mTimeout);
            if (response == null) {
                result = false;
                LOG.warn("WriteRequest for {} on resource {} timeout!", registration.getEndpoint(),
                        request.getPath().toString());
            } else if (response.isSuccess()) {
                result = true;
            } else if (!response.isSuccess()) {
                result = false;
                LOG.warn("WriteRequest for {} on resource {} Failed! Error: {} : {}", registration.getEndpoint(),
                        request.getPath().toString(), response.getCode(), response.getErrorMessage());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
    private void setDevicesPayload(Registration registration, LwM2mNode devicesObject, Map<String, Map<Long, JSONObject>> payloads) {
        if (devicesObject != null && devicesObject instanceof LwM2mObject) {
            for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) devicesObject).getInstances().entrySet()) {
                String serialNr = getStringResource(entry.getValue(), RESOURCE_ID_SERIAL_NUMBER);
                Boolean isReachable = getBooleanResource(entry.getValue(), RESOURCE_ID_REACHABLE);
                Date lastActiveTime = getDateResource(entry.getValue(), RESOURCE_ID_LAST_ACTIVE_TIME);
                Long battery = getIntegerResource(entry.getValue(), RESOURCE_ID_BATTERY);
                Double battery_level = getDoubleResource(entry.getValue(), RESOURCE_ID_BATTERY_LEVEL);

                setValue2payload(serialNr, System.currentTimeMillis(), NAME_REACHABLE, isReachable, payloads);
                setValue2payload(serialNr, System.currentTimeMillis(), NAME_LATEST_TIME, lastActiveTime.getTime(), payloads);
                setValue2payload(serialNr, System.currentTimeMillis(), NAME_BATTERY, battery, payloads);
                setValue2payload(serialNr, System.currentTimeMillis(), NAME_BATTERY_LEVEL, battery_level, payloads);
            }
        }
    }
    private void setAlarmPayload(Registration registration, LwM2mNode AlarmObject, Map<Integer, String> serialMapping, Map<String, Map<Long, JSONObject>> payloads) {
        if (AlarmObject != null && AlarmObject instanceof LwM2mObject) {
            for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) AlarmObject).getInstances().entrySet()) {
                String serialNr = getSerialNumber(registration, serialMapping, entry.getKey());
                Long smokeAlarm = getIntegerResource(entry.getValue(), RESOURCE_ID_SMOKE_ALARM);
                Boolean isHushed = getBooleanResource(entry.getValue(), RESOURCE_ID_HUSHED);
                Boolean isTemperatureAlarm = getBooleanResource(entry.getValue(), RESOURCE_ID_TEMPERATURE_ALARM);
                Boolean isCoAlarm = getBooleanResource(entry.getValue(), RESOURCE_ID_CO_ALARM);

                setValue2payload(serialNr, System.currentTimeMillis(), NAME_SMOKE_ALARM, smokeAlarm, payloads);
                setValue2payload(serialNr, System.currentTimeMillis(), NAME_HUSHED, isHushed, payloads);
                setValue2payload(serialNr, System.currentTimeMillis(), NAME_TEMPERATURE_ALARM, isTemperatureAlarm, payloads);
                setValue2payload(serialNr, System.currentTimeMillis(), NAME_CO_ALARM, isCoAlarm, payloads);

                Double temperature = getDoubleResource(entry.getValue(), RESOURCE_ID_TEMPERATURE);
                Double pressure = getDoubleResource(entry.getValue(), RESOURCE_ID_PRESSURE);
                Long co2 = getIntegerResource(entry.getValue(), RESOURCE_ID_CO2);
                Long co = getIntegerResource(entry.getValue(), RESOURCE_ID_CO);
                Long humidity = getIntegerResource(entry.getValue(), RESOURCE_ID_HUMIDITY);

                setValue2payload(serialNr, System.currentTimeMillis(), NAME_ALARM_TEMPERATURE, temperature, payloads);
                setValue2payload(serialNr, System.currentTimeMillis(), NAME_ALARM_ATMOSPHERIC, pressure, payloads);
                setValue2payload(serialNr, System.currentTimeMillis(), NAME_ALARM_CO2, co2, payloads);
                setValue2payload(serialNr, System.currentTimeMillis(), NAME_ALARM_CO, co, payloads);
                setValue2payload(serialNr, System.currentTimeMillis(), NAME_ALARM_HUMIDITY, humidity, payloads);
            }
        }
    }
    private Map<Integer, String> getSerialMapping(Registration registration, LwM2mNode devicesObject) {
        Map<Integer, String> mapping = null;
        if (devicesObject != null && devicesObject instanceof LwM2mObject) {
            mapping = new HashMap<Integer, String>();
            for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) devicesObject).getInstances().entrySet()) {
                mapping.put(entry.getKey(), getStringResource(entry.getValue(), RESOURCE_ID_SERIAL_NUMBER));
            }
        }
        return mapping;
    }
    
    private JSONObject getRedisEndpointInfo(String endpoint) {
        JSONObject endpointInfo = null;
        String info = mRedisMessage.getEndpointInfo(endpoint);
        LOG.warn("get mapping ({}) : {}", info, endpoint);
        if (info != null) {
            endpointInfo = new JSONObject(info);
        }
        return endpointInfo;
    }
    private JSONObject addToJsonDevicesObject(JSONObject endpointInfo, LwM2mNode devicesObject) {
        JSONObject json = new JSONObject(OnConnectAction.this.gson.toJson(devicesObject));
        if(endpointInfo == null) {
            endpointInfo = new JSONObject();    
        }
        endpointInfo.put(JSON_DEVICES_KEY, json);
        return endpointInfo;
    }
    private JSONObject addToJsonSerialMapping(JSONObject endpointInfo, Map<Integer, String> serialMapping) {
        JSONObject json = new JSONObject();
        for (Map.Entry<Integer, String> entry : serialMapping.entrySet()) {
            json.put(entry.getKey().toString(), entry.getValue());
        }
        if(endpointInfo == null) {
            endpointInfo = new JSONObject();    
        }
        endpointInfo.put(JSON_MAPPING_KEY, json);
        return endpointInfo;
    }
    private void setRedisEndpointInfo(String endpoint, JSONObject endpointInfo) {
        LOG.warn("Push mapping ({}) : {}", endpointInfo.toString(), endpoint);
        mRedisMessage.setEndpointInfo(endpoint, endpointInfo.toString());
    }

    private Map<Integer, String> getFromJsonSerialMapping(JSONObject endpointInfo) {
        Map<Integer, String> endpointMapping = null;
        if (endpointInfo != null) {
            endpointMapping = new HashMap<Integer, String>();
            JSONObject map = endpointInfo.getJSONObject(JSON_MAPPING_KEY);
            Iterator<String> keys = map.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                endpointMapping.put(Integer.valueOf(key), map.get(key).toString());
            }        
        }
        return endpointMapping;
    }

    private String getSerialNumber(Registration registration, Map<Integer, String> deviceMapping, int instanceId) {
        String result = null;
        result = deviceMapping.get(instanceId);
        if (result == null) { // 99.9% unreachable code!
            LwM2mNode devicesObject = readRequest(registration, PATH_DEVICES);
            Map<Integer, String> newDeviceMapping = getSerialMapping(registration, devicesObject);
            if (newDeviceMapping != null && newDeviceMapping.get(instanceId) != null) {
                result = newDeviceMapping.get(instanceId);
                deviceMapping.clear();
                deviceMapping.putAll(newDeviceMapping);    
                if(mRedisMessage != null) { 
                    JSONObject endpointInfo = getRedisEndpointInfo(registration.getEndpoint());
                    addToJsonSerialMapping(endpointInfo, deviceMapping);
                    setRedisEndpointInfo(registration.getEndpoint(), endpointInfo);
                }
            }
        }
        return result;
    }

    private void processData(Registration registration, Map<Integer, String> deviceMapping, String resourceLink,
            Map<String, Map<Long, JSONObject>> payloads) {
        LwM2mNode object = readRequest(registration, resourceLink);
        if (object != null && object instanceof LwM2mObject) {
            LwM2mObject obj = (LwM2mObject) object;
            String sensorName = getSensorName(obj.getId());
            for (Map.Entry<Integer, LwM2mObjectInstance> entry : obj.getInstances().entrySet()) {
                Long pulse = getIntegerResource(entry.getValue(), RESOURCE_ID_PULSE);
                Map<Integer, Object> resourceMap = getResourceMap(entry.getValue(), RESOURCE_ID_RESOURCE_MAP);
                Date lmt = getDateResource(entry.getValue(), RESOURCE_ID_LMT);
                String serialNumber = getSerialNumber(registration, deviceMapping, entry.getKey());
                if (serialNumber != null && lmt != null && resourceMap != null && pulse != null
                        && resourceMap.size() > 0) {
                    createPayload(serialNumber, lmt, resourceMap, sensorName, pulse, payloads);
                    clearInstanceAsync(registration, obj.getId(), entry.getValue());
                } else {
                    LOG.warn(
                        "CreatePayload Skipped for {}. SensorName: {}; serialNumber: {}; Last measurement time is {} null; resourceMap is {} null; pulse is {} null; resourceMap.size(): {};",
                        registration.getEndpoint(), sensorName, serialNumber, (lmt == null ? "" : "not"), (resourceMap == null ? "" : "not"), (pulse == null ? "" : "not"),
                        (resourceMap != null ? resourceMap.size() : "Null"));
                }
            }
            //clearObjectAsync(registration, obj);
        }
    }

    private void createPayload(String serialNumber, Date lmt, Map<Integer, Object> resourceMap, String resName,
            Long pulse, Map<String, Map<Long, JSONObject>> payloads) {
        Long defTime = lmt.getTime();
        Map<Long, JSONObject> payloadMap = payloads.get(serialNumber);
        if (payloadMap == null) {
            payloadMap = new HashMap<Long, JSONObject>();
            payloads.put(serialNumber, payloadMap);
        }
        for (Map.Entry<Integer, Object> resEntry : resourceMap.entrySet()) {
            Long calcTime = (defTime - (((resourceMap.size() - 1) - resEntry.getKey()) * 1000 * pulse));
            JSONObject payloadObj = payloadMap.get(calcTime);
            if (payloadObj == null) {
                payloadObj = new JSONObject("{\"" + resName + "\":" + resEntry.getValue() + "}");
                payloadMap.put(calcTime, payloadObj);
            } else {
                payloadObj.put(resName, resEntry.getValue());
            }
            // String payload = "{\"ts\":" + calcTime +",\"values\":{\"temperature\":" +
            // resEntry.getValue() + ", \"co2\":"+ co2 +"}}";
        }
    }   
    private void setValue2payload(String serialNumber, Long tim, String resName, Object value, Map<String, Map<Long, JSONObject>> payloads) {
        //Serial payloads
        Map<Long, JSONObject> payloadMap = payloads.get(serialNumber);
        if (payloadMap == null) {
            payloadMap = new HashMap<Long, JSONObject>();
            payloads.put(serialNumber, payloadMap);
        }
        //time payloads
        JSONObject payloadObj = payloadMap.get(tim);
        if (payloadObj == null) {
            payloadObj = new JSONObject("{\"" + resName + "\":" + value + "}");
            payloadMap.put(tim, payloadObj);
        } else {
            payloadObj.put(resName, value);
        }
    }

    private void clearObjectAsync(Registration registration, LwM2mObject object) {
        final CountDownLatch latch = new CountDownLatch(object.getInstances().size());
        for (Map.Entry<Integer, LwM2mObjectInstance> entry : object.getInstances().entrySet()) {
            Date lmt = getDateResource(entry.getValue(), RESOURCE_ID_LMT);
            if (lmt != null) {
                WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, object.getId(), entry.getKey(),
                        LwM2mSingleResource.newDateResource(RESOURCE_ID_LRMT, lmt));
                final String debug = registration.getEndpoint() + ":" + object.getId() + " / " + request.getPath().toString();
                this.mLeshanServer.send(registration, request, this.mTimeout, new ResponseCallback<WriteResponse>() {
                    @Override
                    public void onResponse(WriteResponse response) {
                        latch.countDown();
                        LOG.warn("onResponse  {} : {} ", response.isSuccess(), debug);
                    }
                }, new ErrorCallback() {
                    @Override
                    public void onError(Exception e) {
                        LOG.warn("onError {} : {}", debug, e.getMessage());
                    }
                });
            } else {
                latch.countDown();
                LOG.warn("Clear object skipped for {} on object {}/{} because last measurement time is null",
                        registration.getEndpoint(), object.getId(), entry.getKey());
            }

        }
        LOG.warn("latch.await for {} : {}", registration.getEndpoint(), object.getId());
        try {
            Boolean result = latch.await(this.mTimeout, TimeUnit.MILLISECONDS);
            if(!result) {
                LOG.warn("latch.await ended with timeout for {} : {}", registration.getEndpoint(), object.getId());     
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOG.warn("latch.await ended for {} : {}", registration.getEndpoint(), object.getId());
    }

    private void clearInstanceAsync(Registration registration, Integer objectId, LwM2mObjectInstance inst) {
        Date lmt = getDateResource(inst, RESOURCE_ID_LMT);
        if (lmt != null) {
            WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, objectId, inst.getId(),
                    LwM2mSingleResource.newDateResource(RESOURCE_ID_LRMT, lmt));
            final String debug = registration.getEndpoint() + ":" + objectId + " / " + request.getPath().toString();
            this.mLeshanServer.send(registration, request, this.mTimeout, new ResponseCallback<WriteResponse>() {
                @Override
                public void onResponse(WriteResponse response) {
                    if(!response.isSuccess()) {
                        LOG.warn("onResponse  {} : {} ", response.isSuccess(), debug);
                    }
                }
            }, new ErrorCallback() {
                @Override
                public void onError(Exception e) {
                    LOG.warn("onError {} : {}", debug, e.getMessage());
                }
            });
        } else {
            LOG.warn("Clear object skipped for {} on object {}/{} because last measurement time is null",
                    registration.getEndpoint(), objectId, inst.getId());
        }
    }
    private Boolean clearInstance(Registration registration, Integer objectId, LwM2mObjectInstance inst) {
        Boolean result = false;
        Date lmt = getDateResource(inst, RESOURCE_ID_LMT);
        if (lmt != null) {
            WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, objectId, inst.getId(),
                    LwM2mSingleResource.newDateResource(RESOURCE_ID_LRMT, lmt));
            result = writeRequest(registration, request);
        } else {
            LOG.warn("Clear object skipped for {} on object {}/{} because last measurement time is null",
            registration.getEndpoint(), objectId, inst.getId());
        }
        return result;
    }
    private Boolean getBooleanResource(LwM2mObjectInstance instObj, int res) {
        Boolean result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.BOOLEAN)) {
                result = (Boolean) lmtRes.getValue();
            }
        }
        return result;
    }

    private String getStringResource(LwM2mObjectInstance instObj, int res) {
        String result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.STRING)) {
                result = (String) lmtRes.getValue();
            }
        }
        return result;
    }

    private Date getDateResource(LwM2mObjectInstance instObj, int res) {
        Date result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.TIME)) {
                result = (Date) lmtRes.getValue();
            }
        }
        return result;
    }

    private Long getIntegerResource(LwM2mObjectInstance instObj, int res) {
        Long result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.INTEGER)) {
                result = (Long) lmtRes.getValue();
            }
        }
        return result;
    }
    private Double getDoubleResource(LwM2mObjectInstance instObj, int res) {
        Double result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.FLOAT)) {
                result = (Double) lmtRes.getValue();
            }
        }
        return result;
    }

    private Map<Integer, Object> getResourceMap(LwM2mObjectInstance instObj, int inst) {
        Map<Integer, LwM2mResource> resources = instObj.getResources();
        LwM2mMultipleResource resource = (LwM2mMultipleResource) resources.get(inst);
        @SuppressWarnings("unchecked")
        Map<Integer, Object> resourceMap = (Map<Integer, Object>) resource.getValues();
        return resourceMap;
    }
    //================================================================================
    // For Redis instructions!
    //================================================================================
    private Map<String, ArrayList<String>> serializePayloadAll(Map<String, Map<Long, JSONObject>> dataMap) {
        Map<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
        for (Map.Entry<String, Map<Long, JSONObject>> entry : dataMap.entrySet()) {
            String payloads = serializePayloads(entry.getValue());
            ArrayList<String> payloadList = new ArrayList<String>();
            payloadList.add(payloads);
            result.put(entry.getKey(), payloadList);
        }
        return result;
    }

    private String serializePayloads(Map<Long, JSONObject> data) {
        JSONArray array = new JSONArray();
        for (Map.Entry<Long, JSONObject> entry : data.entrySet()) {
            JSONObject superObj = new JSONObject();
            superObj.put("ts", entry.getKey());
            superObj.put("values", entry.getValue());
            array.put(superObj);
        }
        return array.toString();
    }

    private void processRedisRequests(Registration registration, Map<Integer, String> resourceMap) {
        Map<String, String> requestPayload = this.mRedisMessage.getEndpointRequests(registration.getEndpoint());
        if (requestPayload != null) {
            for (Map.Entry<String, String> entry : requestPayload.entrySet()) {
                RedisRequestLink requestLink = new RedisRequestLink(entry.getKey(), entry.getValue());
                if (!requestLink.isError()) {
                    sendRequest(registration, requestLink);
                }
                entry.setValue(requestLink.getResponse());
            }
            this.mRedisMessage.sendResponse(registration.getEndpoint(), requestPayload);
        }
    }

    private void sendRequest(Registration registration, RedisRequestLink objLink) {
        try {
            if (objLink.isRead()) {
                ReadResponse response = this.mLeshanServer.send(registration, new ReadRequest(objLink.getLink()),
                        this.mTimeout);
                // set read values
                if (response == null) {
                    JSONObject res = new JSONObject();
                    res.put("error", "timeout");
                    objLink.setResponse(res);
                } else if (response.isSuccess()) {
                    objLink.setResponse(OnConnectAction.this.gson.toJson(response.getContent()));
                } else {
                    JSONObject res = new JSONObject();
                    res.put("error", response.getErrorMessage());
                    res.put("code", response.getCode());
                    objLink.setResponse(res);
                }
            } else if (objLink.isWrite()) {
                ResourceModel resourceModel = this.mLeshanServer.getModelProvider().getObjectModel(registration)
                        .getObjectModel(objLink.getObjectId()).resources.get(objLink.getResourceId());
                if (resourceModel != null) {
                    try {
                        Object value = null;
                        if (resourceModel.type.equals(org.eclipse.leshan.core.model.ResourceModel.Type.BOOLEAN)) {
                            value = Boolean.valueOf(objLink.getValue().toString());
                        } else if (resourceModel.type
                                .equals(org.eclipse.leshan.core.model.ResourceModel.Type.INTEGER)) {
                            value = Long.valueOf(objLink.getValue().toString());
                        } else if (resourceModel.type.equals(org.eclipse.leshan.core.model.ResourceModel.Type.STRING)) {
                            value = String.valueOf(objLink.getValue().toString());
                        }
                        if (value != null) {
                            WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, objLink.getObjectId(),
                                    objLink.getInstanceId(), LwM2mSingleResource.newResource(objLink.getResourceId(),
                                            value, resourceModel.type));
                            // blocking request
                            WriteResponse response = this.mLeshanServer.send(registration, request, this.mTimeout);
                            if (response == null) {
                                JSONObject res = new JSONObject();
                                res.put("error", "timeout");
                                objLink.setResponse(res);
                            } else if (response.isSuccess()) {
                                JSONObject res = new JSONObject();
                                res.put("result", response.getCoapResponse().toString());
                                objLink.setResponse(res);
                            } else {
                                JSONObject res = new JSONObject();
                                res.put("error", response.getErrorMessage());
                                res.put("code", response.getCode());
                                objLink.setResponse(res);
                            }
                        } else {
                            JSONObject res = new JSONObject();
                            res.put("error", "Resource type not implemented! " + resourceModel.type);
                            objLink.setResponse(res);
                        }
                    } catch (Exception e) {
                        JSONObject res = new JSONObject();
                        res.put("error", "Write value cast error: " + objLink.getValue());
                        objLink.setResponse(res);
                    }
                } else {
                    JSONObject res = new JSONObject();
                    res.put("error", "Resource model not found! " + objLink.getObjectId() + objLink.getResourceId());
                    objLink.setResponse(res);
                }
            } else {
                JSONObject res = new JSONObject();
                res.put("error", "Action not implemented!");
                objLink.setResponse(res);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}