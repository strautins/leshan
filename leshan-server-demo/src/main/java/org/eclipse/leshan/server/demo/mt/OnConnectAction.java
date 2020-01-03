package org.eclipse.leshan.server.demo.mt;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnConnectAction {

    private static final Logger LOG = LoggerFactory.getLogger(OnConnectAction.class);

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private static final String PATH_DEVICES = "/33756/";
    public static final int OBJECT_ID_DEVICES = 33756;

    protected static final int OBJECT_ID_GROUP = 33755;
    private static final String PATH_GROUP = "/33755/";
    private static final String PATH_GROUP_ATTENTION_REQUIRED = "33755/0/0";
    private static final String PATH_GROUP_ALARM_TRIGGERED = "33755/0/1";
    private static final String PATH_GROUP_DEVICES_INFO_CHANGED = "33755/0/2";
    /** Sensor object temperature */
    protected static final int OBJECT_ID_TEMPERATURE = 33758;
    private static final String PATH_TEMPERATURE = "/33758/";
    private static final String NAME_TEMPERATURE = "temperature";
    /** Sensor object humidity */
    protected static final int OBJECT_ID_HUMIDITY = 33759;
    private static final String PATH_HUMIDITY = "/33759/";
    private static final String NAME_HUMIDITY = "humidity";
    /** Sensor object co2 */
    protected static final int OBJECT_ID_CO2 = 33761;
    private static final String PATH_CO2 = "/33761/";
    private static final String NAME_CO2 = "co2";
    /** Sensor object co */
    protected static final int OBJECT_ID_CO = 33762;
    private static final String PATH_CO = "/33762/";
    private static final String NAME_CO = "co";
    /** Sensor object atmospheric */
    protected static final int OBJECT_ID_PRESSURE = 33760;
    private static final String PATH_PRESSURE = "/33760/";
    private static final String NAME_PRESSURE = "atmospheric";
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

    protected static final int RESOURCE_ID_TEMPERATURE_ENABLE = 11;
    public static final int RESOURCE_ID_TEMPERATURE_INTERVAL = 12;
    protected static final int RESOURCE_ID_HUMIDITY_ENABLE = 13;
    public static final int RESOURCE_ID_HUMIDITY_INTERVAL = 14;
    protected static final int RESOURCE_ID_PRESSURE_ENABLE = 15;
    public static final int RESOURCE_ID_PRESSURE_INTERVAL = 16;
    protected static final int RESOURCE_ID_CO2_ENABLE = 17;
    public static final int RESOURCE_ID_CO2_INTERVAL = 18;
    protected static final int RESOURCE_ID_CO_ENABLE = 19;
    public static final int RESOURCE_ID_CO_INTERVAL = 20;

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

    /** Group resource ID */
    private static final int RESOURCE_ID_ALARM_TRIGGERED = 1;
    private static final int RESOURCE_ID_DEVICES_INFO_CHANGED = 2;
    /** dummy resource name */
    private static final String NAME_NONE = "none";

    private final LeshanServer mLeshanServer;
    private final ThingsboardSend mThingsboardSend;
    private final RedisStorage mRedisStorage;
    private final SimpleCache mSimpleCache;
    private final long mTimeout;

    private final Gson gson;

    public OnConnectAction(LeshanServer leshanServer, ThingsboardSend thingsboardSend, RedisStorage redisStorage) throws URISyntaxException {
        this.mLeshanServer = leshanServer;
        this.mRedisStorage = redisStorage;
        this.mSimpleCache = new InMemoryCache();
        this.mThingsboardSend = thingsboardSend;
        this.mTimeout = 10000;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
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

    public SimpleCache getSimpleCache() {
        return mSimpleCache;
    }

    private String getSensorName(Integer objectId) {
        String result = null;
        switch (objectId) {
        case OBJECT_ID_TEMPERATURE:
            result = NAME_TEMPERATURE;
            break;
        case OBJECT_ID_HUMIDITY:
            result = NAME_HUMIDITY;
            break;
        case OBJECT_ID_PRESSURE:
            result = NAME_PRESSURE;
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

    private void wrapperGetResources(Registration registration, String event) {
        if (event.equals(EVENT_REGISTRATION) || event.equals(EVENT_UPDATED)) {
            if (this.mSimpleCache.lock(registration.getEndpoint())) {
                try {
                    LOG.debug("Lock acquired for {} with {}; Thread: {}", registration.getEndpoint(), event,
                            Thread.currentThread().getName());
                    if(registration.getObjectLinks() != null) {
                        getResources(registration, event);
                    }
                } finally {
                    this.mSimpleCache.unlock(registration.getEndpoint());
                    LOG.debug("Unlock for {} with {}; Thread: {}", registration.getEndpoint(), event, Thread.currentThread().getName());    
                }
            } else {
                LOG.debug("Lock not acquired {} with {};", registration.getEndpoint(), event);    
            }
        }  else {
            Boolean remove = mSimpleCache.delEndpointCache(registration.getEndpoint());
            LOG.debug("Remove Cache for {} : {}", registration.getEndpoint(), remove);
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
            } else if (i.getUrl().contains(PATH_PRESSURE)) {
                isAtmospheric = true;
            } else if (i.getUrl().contains(PATH_ALARM)) {
                isAlertObject = true;
            } else if (i.getUrl().contains(PATH_DEVICES)) {
                isDevices = true;
            }
        }
        //testing
        // if (isDevices) {
        //     ResourceModel resourceModel = this.mLeshanServer.getModelProvider().getObjectModel(registration)
        //         .getObjectModel(OBJECT_ID_DEVICES).resources.get(9);
        //     LOG.debug("what the heaven for {} ", resourceModel.toString());
        //     multiWriteRequest(registration);
        // }
        if (isMainObj && isDevices) {
            Boolean isAttentionRequired = readBooleanResource(registration, PATH_GROUP_ATTENTION_REQUIRED);
            if (isAttentionRequired == null) {
                // error occurred while getting flag from device;
            } else {
                EndpointCache endpointCache = null;
                if(event.equals(EVENT_REGISTRATION)) {
                    endpointCache = null;    
                } else {
                    endpointCache = mSimpleCache.getEndpointCache(registration.getEndpoint());
                }

                // if attention required or serial mapping is missing
                if (isAttentionRequired || endpointCache == null) {
                    Boolean isAlarmTriggered = false;
                    Boolean isDevicesInfoChanged = false;
                    if (isAttentionRequired) {
                        isAlarmTriggered = readBooleanResource(registration, PATH_GROUP_ALARM_TRIGGERED);
                        isDevicesInfoChanged = readBooleanResource(registration,
                                PATH_GROUP_DEVICES_INFO_CHANGED);
                    }
                    //on object instance multi resource write
                    // WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, OBJECT_ID_GROUP,
                    //             0,
                    //             LwM2mSingleResource.newBooleanResource(RESOURCE_ID_ALARM_TRIGGERED, false),
                    //             LwM2mSingleResource.newBooleanResource(RESOURCE_ID_DEVICES_INFO_CHANGED, false));
                    // writeRequest(registration, request);

                    if (isDevicesInfoChanged != null && isDevicesInfoChanged || endpointCache == null) {
                        // clear flag aware of changes
                        if (isDevicesInfoChanged) {
                            WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, OBJECT_ID_GROUP,
                                    0, LwM2mSingleResource.newBooleanResource(RESOURCE_ID_DEVICES_INFO_CHANGED,
                                            false));
                            writeRequest(registration, request);
                        }
                        // read devices object, get mapping collect payloads for battery, reachability,
                        // last activity time
                        LwM2mNode devicesObject = readRequest(registration, PATH_DEVICES);
                        if(devicesObject == null) {
                            LOG.error("Exiting process for endpoint:{} Reason: Response of resource {} is null", registration.getEndpoint(), PATH_DEVICES);
                            return;
                        }
                        endpointCache = new EndpointCache(registration.getEndpoint(), devicesObject);
                        endpointCache.createDevicesPayload(devicesObject);
                        // push mapping to cache
                        mSimpleCache.setEndpointCache(registration.getEndpoint(), endpointCache);
                    }

                    if (isAlarmTriggered != null && isAlarmTriggered && isAlertObject && endpointCache != null) {
                        // todo maybe observe all object check changes!
                        LwM2mNode alarmObject = readRequest(registration, PATH_ALARM);
                        boolean isAlarm = endpointCache.createAlarmPayload(alarmObject);
                        //clearing flag
                        if(isAlarmTriggered && !isAlarm) {
                            WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, OBJECT_ID_GROUP,
                                    0,
                                    LwM2mSingleResource.newBooleanResource(RESOURCE_ID_ALARM_TRIGGERED, false));
                            writeRequest(registration, request);
                        }
                    }
                }

                if (endpointCache != null) {
                    if (isTemperature) {
                        processData(registration, endpointCache, PATH_TEMPERATURE);
                    }
                    if (isHumidity) {
                        processData(registration, endpointCache, PATH_HUMIDITY);
                    }
                    if (isCo2) {
                        processData(registration, endpointCache, PATH_CO2);
                    }
                    if (isCo) {
                        processData(registration, endpointCache, PATH_CO);
                    }
                    if (isAtmospheric) {
                        processData(registration, endpointCache, PATH_PRESSURE);
                    }
                    sendAll(endpointCache.serializePayloadAll());
                }

            }
        }
        // collecting request from redis
        if (this.mRedisStorage != null) {
            processRedisRequests(registration);
        }
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
 
    // private JSONObject addToJsonDevicesObject(JSONObject endpointCache, LwM2mNode
    // devicesObject) {
    // JSONObject json = new
    // JSONObject(OnConnectAction.this.gson.toJson(devicesObject));
    // if(endpointCache == null) {
    // endpointCache = new JSONObject();
    // }
    // endpointCache.put(JSON_DEVICES_KEY, json);
    // return endpointCache;
    // }

    private void processData(Registration registration, EndpointCache endpointCache, String resourceLink) {
        LwM2mNode object = readRequest(registration, resourceLink);
        if (object != null && object instanceof LwM2mObject) {
            LwM2mObject obj = (LwM2mObject) object;
            String sensorName = getSensorName(obj.getId());
            for (Map.Entry<Integer, LwM2mObjectInstance> entry : obj.getInstances().entrySet()) {
                Long pulse = endpointCache.getIntervalKey(entry.getKey(), object.getId());
                Map<Integer, Object> resourceMap = getResourceMap(entry.getValue(), RESOURCE_ID_RESOURCE_MAP);
                Date fmt = getDateResource(entry.getValue(), RESOURCE_ID_FMT);
                String serialNumber = endpointCache.getSerial(entry.getKey());
                if (serialNumber != null && fmt != null && resourceMap != null && pulse != null
                        && resourceMap.size() > 0) {
                    LOG.debug("CreatePayload for {}. SensorName: {}; serialNumber: {}; first measurement time: {}; pulse: {}; resourceMap.size(): {};",
                                registration.getEndpoint(), sensorName, serialNumber, fmt.getTime(), pulse, resourceMap.size());        
                    endpointCache.createPayload(serialNumber, fmt, resourceMap, sensorName, pulse);
                    clearInstanceAsync(registration, obj.getId(), entry.getValue(), (resourceMap.size() * pulse * 1000));
                } else {
                    LOG.debug(
                            "CreatePayload Skipped for {}. SensorName: {}; serialNumber: {}; first measurement time is {} null; resourceMap is {} null; pulse is {} null; resourceMap.size(): {};",
                            registration.getEndpoint(), sensorName, serialNumber, (fmt == null ? "" : "not"),
                            (resourceMap == null ? "" : "not"), (pulse == null ? "" : "not"),
                            (resourceMap != null ? resourceMap.size() : "Null"));
                }
            }
            // clearObjectAsync(registration, obj);
        }
    }

    private void clearInstanceAsync(Registration registration, Integer objectId, LwM2mObjectInstance inst, long addTime) {
        Date fmt = getDateResource(inst, RESOURCE_ID_FMT);
        if (fmt != null) {
            Date lmt = new Date(fmt.getTime()  + addTime);
            ExecuteRequest request = new ExecuteRequest(objectId, inst.getId(), RESOURCE_ID_CLEAR_MEASUREMENTS,
                    String.valueOf(lmt.getTime() / 1000));
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
        } else {
            LOG.debug("Clear object skipped for {} on object {}/{} because last measurement time is null",
                    registration.getEndpoint(), objectId, inst.getId());
        }
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
        // if (this.mRedisMessage != null) {
        // this.mRedisMessage.sendPayload(data);
        // for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
        // this.mRedisMessage.writeEventList(entry.getKey());
        // }
        // }
    }
    //================================================================================
    // For Redis instructions!
    //================================================================================
    private void processRedisRequests(Registration registration) {
        Map<String, String> requestPayload = this.mRedisStorage.getEndpointRequests(registration.getEndpoint());
        if (requestPayload != null) {
            for (Map.Entry<String, String> entry : requestPayload.entrySet()) {
                RedisRequestLink requestLink = new RedisRequestLink(entry.getKey(), entry.getValue());
                if (!requestLink.isError()) {
                    sendRequest(registration, requestLink);
                }
                entry.setValue(requestLink.getResponse());
            }
            this.mRedisStorage.sendResponse(registration.getEndpoint(), requestPayload);
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
                        String version = this.mLeshanServer.getModelProvider().getObjectModel(registration)
                        .getObjectModel(objLink.getObjectId()).getVersion();
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