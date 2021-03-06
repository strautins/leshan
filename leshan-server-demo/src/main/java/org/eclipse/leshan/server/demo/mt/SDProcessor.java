package org.eclipse.leshan.server.demo.mt;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.interceptors.MessageInterceptorAdapter;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.mt.tb.ThingsboardSend;
import org.eclipse.leshan.server.demo.mt.scheduler.RequestPayload;
import org.eclipse.leshan.server.demo.mt.scheduler.ScheduleRequest;
import org.eclipse.leshan.server.demo.mt.scheduler.ScheduleRequestDeserializer;
import org.eclipse.leshan.server.demo.mt.scheduler.ScheduleRequestSerializer;
import org.eclipse.leshan.server.demo.mt.dbg.MyMessageTracer;
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
import org.mikrotik.iot.sd.utils.PacketConfig;
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
    // protected static final String NAME_BATTERY = "battery";
    // protected static final String NAME_BATTERY_LEVEL = "battery_level";
    // protected static final String NAME_LATEST_TIME = "latest_time";
    // protected static final String NAME_REACHABLE = "reachable";
    /** Devices resource ID */
    protected static final int RESOURCE_ID_SERIAL_NUMBER = 2;
    // protected static final int RESOURCE_ID_REACHABLE = 3;
    // protected static final int RESOURCE_ID_LAST_ACTIVE_TIME = 4;
    // protected static final int RESOURCE_ID_BLUETOOTH_SIGNAL = 5;
    // protected static final int RESOURCE_ID_BATTERY = 6;
    // protected static final int RESOURCE_ID_BATTERY_LEVEL = 7;

    /** Alarm resource names */
    // protected static final String NAME_SMOKE_ALARM = "smoke_alarm";
    // protected static final String NAME_HUSHED = "hushed";
    // protected static final String NAME_TEMPERATURE_ALARM = "temperature_alarm";
    // protected static final String NAME_CO_ALARM = "co_alarm";

    // /** Alarm object resource ID */
    // protected static final int RESOURCE_ID_SMOKE_ALARM = 0;
    // protected static final int RESOURCE_ID_CO_ALARM = 1;
    // protected static final int RESOURCE_ID_TEMPERATURE = 2;
    // protected static final int RESOURCE_ID_HUSHED = 3;

    private final LeshanServer mLeshanServer;
    private final ThingsboardSend mThingsboardSend;
    private final SimpleStorage mSimpleStorage;
    // to push scheduled Read to interface
    private EventServlet mEventServlet = null;

    private final long mTimeout;

    private final Gson gson;

    public SDProcessor(LeshanServer leshanServer, ThingsboardSend thingsboardSend, SimpleStorage simpleStorage)
            throws URISyntaxException {
        this.mLeshanServer = leshanServer;
        this.mSimpleStorage = simpleStorage;
        this.mThingsboardSend = thingsboardSend;
        this.mTimeout = 15000;

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

    /** For interface notify of scheduled execution results */
    public void setEventServlet(EventServlet event) {
        this.mEventServlet = event;
    }

    /** Add registration listener for handling SD devices */
    public void start() {
        if (this.mThingsboardSend != null) {
            this.mThingsboardSend.start();
        }
        this.mLeshanServer.getRegistrationService().addListener(this.registrationListener);
        this.mLeshanServer.coap().getSecuredEndpoint().addInterceptor(messageInterceptorAdapter);
        this.mLeshanServer.coap().getSecuredEndpoint().addInterceptor(myMessageTracer);

    }

    /** Remove registration listener */
    public void stop() {
        if (this.mThingsboardSend != null) {
            this.mThingsboardSend.stop();
        }
        this.mLeshanServer.getRegistrationService().removeListener(this.registrationListener);
        this.mLeshanServer.coap().getSecuredEndpoint().removeInterceptor(messageInterceptorAdapter);
        this.mLeshanServer.coap().getSecuredEndpoint().removeInterceptor(myMessageTracer);
    }

    private void wrapperGetResources(Registration registration, String event) {
        if (event.equals(EVENT_REGISTRATION) || event.equals(EVENT_UPDATED)) {
            if (registration.getObjectLinks() != null) {
                long start = System.currentTimeMillis();
                getResources(registration, event);
                LOG.info("Process of Endpoint {} took {} ms", registration.getEndpoint(),
                        (System.currentTimeMillis() - start));
            }
        }
    }

    /** Main SD device handling demo */
    private void getResources(Registration registration, String event) {
        boolean isMainObj = false;
        boolean isDevices = false;
        boolean isSensors = false;
        boolean isAlarm = false;
        // From Registration info extract device objects
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
        // if is objects of SD consider it is SD device.
        if (isMainObj && isDevices) {
            LwM2mObject devicesObject = null;
            LwM2mObject groupObj = null;
            LwM2mObject AlarmObj = null;
            boolean needToClearData = false;
            byte[] events = null;

            // On REGISTRATION collect info about device, check and alter desired config...
            if (event.equals(EVENT_REGISTRATION)) {
                groupObj = (LwM2mObject) Lwm2mHelper.readRequest(this.mLeshanServer, registration,
                        PATH_GROUP.toString(), this.mTimeout);
                if (groupObj != null) {
                    putLwM2mObjectInMemory(registration.getEndpoint(), groupObj);
                    events = Lwm2mHelper.getOpaque(groupObj.getInstance(PATH_GROUP_EVENTS.getObjectInstanceId()),
                            PATH_GROUP_EVENTS.getResourceId());
                    byte[] cfg = Lwm2mHelper.getOpaque(groupObj.getInstance(PATH_GROUP_EVENT_CFG.getObjectInstanceId()),
                            PATH_GROUP_EVENT_CFG.getResourceId());
                    cfg = getEndpointCustomEventCfg(registration, cfg);// get byte cfg for enpoint
                    Lwm2mHelper.writeOpaque(this.mLeshanServer, registration, PATH_GROUP_EVENT_CFG, cfg, this.mTimeout);// write bytecfg
                }

                devicesObject = (LwM2mObject) Lwm2mHelper.readRequest(this.mLeshanServer, registration,
                        PATH_DEVICES.toString(), this.mTimeout);
                if (devicesObject != null) {
                    putLwM2mObjectInMemory(registration.getEndpoint(), devicesObject);
                    Map<Integer, byte[]> cfg = getEndpointOutputCfg(registration);
                    for (Map.Entry<Integer, LwM2mObjectInstance> entry : devicesObject.getInstances().entrySet()) {
                        Lwm2mHelper.writeOpaque(this.mLeshanServer, registration,
                                new LwM2mPath(devicesObject.getId(), entry.getKey(), 10), cfg, this.mTimeout);
                    }
                }
            } else {
                events = Lwm2mHelper.readOpaque(this.mLeshanServer, registration, PATH_GROUP_EVENTS, this.mTimeout);
            }

            // While sleeping client collects events happening in devices, you should do
            // something wise with them.
            if (events != null) {
                for (byte[] b : ByteUtil.split(events, 4)) {
                    PredefinedEvent ev = new PredefinedEvent(b);
                    LOG.info("EVENT: {}", ev.toString());
                    if (!ev.getEventCode().equals(EventCode.NO_EVENT)) {
                        needToClearData = true;
                    }
                    if (ev.getEventCode().equals(EventCode.ALARM) && isAlarm) {
                        AlarmObj = (LwM2mObject) Lwm2mHelper.readRequest(this.mLeshanServer, registration,
                                PATH_ALARM.toString(), this.mTimeout);
                        if (AlarmObj != null) {
                            putLwM2mObjectInMemory(registration.getEndpoint(), AlarmObj);
                        }
                    }
                }
            }

            // collect and clear daily sensor data
            if (isSensors) {
                Payload payload = new Payload();
                // In each sensor resource last payload packet, one of bits informs about "has
                // more data"
                // to not break server adding max calls
                int safety = 0;
                while (payload.isRepeatCall() && safety < 5) {
                    safety++;
                    payload.init();
                    // collects all data
                    processData(this.mLeshanServer, registration, PATH_SENSORS.toString(), payload, this.mTimeout);
                    if (payload.isData()) {
                        if (devicesObject == null) {
                            devicesObject = getLwM2mObjectFromMemory(registration.getEndpoint(),
                                    PATH_DEVICES.getObjectId());
                        }
                        if (devicesObject == null) {
                            devicesObject = (LwM2mObject) Lwm2mHelper.readRequest(this.mLeshanServer, registration,
                                    PATH_DEVICES.toString(), this.mTimeout);
                        }
                        // get device serial info, parse collected data in array
                        if (devicesObject != null) {
                            // <serialNr, PayloadList>
                            Map<String, ArrayList<String>> data = new HashMap<String, ArrayList<String>>();
                            // loop over all instances, collect data
                            for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) devicesObject)
                                    .getInstances().entrySet()) {
                                String serialNr = Lwm2mHelper.getStringResource(entry.getValue(),
                                        SDProcessor.RESOURCE_ID_SERIAL_NUMBER);
                                String p = payload.getPayload(entry.getKey());
                                if (p != null && serialNr != null) {
                                    // currently collects all data in json array string. ArrayList not used
                                    ArrayList<String> arr = new ArrayList<String>();
                                    arr.add(p);
                                    data.put(serialNr, arr);
                                }
                            }
                            // send info to backend
                            sendAll(data);
                            // clear read data
                            if (Lwm2mHelper.executeRequest(this.mLeshanServer, registration,
                                    PATH_GROUP_CLEAR_DATA.toString(), null, this.mTimeout)) {
                                needToClearData = false;
                            } else { // if request failed break loop
                                LOG.error("Clear Sensor data failed for Endpoint:{}", registration.getEndpoint());
                                if (payload.isRepeatCall()) {
                                    LOG.error("Breaking sensor read loop for Endpoint:{}", registration.getEndpoint());
                                    break;
                                }
                            }
                        } else {
                            LOG.error("Could not get serial info from Endpoint:{}", registration.getEndpoint());
                            break;
                        }
                    }
                }
                // warn admin
                if (safety >= 5) {
                    LOG.error("Something is wrong in Endpoint:{}. Sensor reading calls reached {} times.",
                            registration.getEndpoint(), safety);
                }
            }

            // clear read event and / or sensor data
            if (needToClearData) {
                Lwm2mHelper.asyncExecuteRequest(this.mLeshanServer, registration, PATH_GROUP_CLEAR_DATA.toString(),
                        null, this.mTimeout);
            }
        }

        // Execute scheduled requests from interface when client was not reachable.
        processScheduledOperations(registration);
    }

    // All bytes stored in LITTLE_ENDIAN byte decode
    // fixed 8 bytes
    // CustomEvent = {
    // 1B CustomEventCode; // configurable event code
    // 1B {
    // 4b reserved / not used
    // 3b crossing type // up(0), down(1), both(2)
    // 1b immediate notify // notify server immediately when this event triggers
    // }
    // 2B affected instances // enabled device bitfield(little_endian) 0010 0101 =
    // 0,2,5
    // 4B value // float type;
    // }
    // payload = [CustomEvent,CustomEvent...]
    // todo:check current config, push desired config..
    public byte[] getEndpointCustomEventCfg(Registration registration, byte[] currentByteCfg) {
        CustomEvent ev1 = new CustomEvent(CodeWrapper.EventCode.TEMP_EVENT, CustomEvent.EventTriggerType.UP, true,
                20.34f, 1, 2);
        CustomEvent ev2 = new CustomEvent(CodeWrapper.EventCode.TEMP_EVENT, CustomEvent.EventTriggerType.BOTH, false,
                10.88f, 0, 2, 4);
        CustomEvent ev3 = new CustomEvent(CodeWrapper.EventCode.TEMP_EVENT, CustomEvent.EventTriggerType.DOWN, true, -5,
                0, 1);
        return ByteUtil.concatenate(ev1.toWriteByte(), ev2.toWriteByte(), ev3.toWriteByte());
    }

    // All bytes stored in LITTLE_ENDIAN byte decode
    // fixed 11 bytes
    // OutputStateConfig = {
    // 1B EventCode
    // 4B Value
    // 1B EventCode2
    // 4B Value2
    // 1B {
    // 4b reserved / not used
    // 1b type // condition type for source 1 ">="(1) "<"(0)
    // 1b type2 // condition type for source 1 ">="(1) "<"(0)
    // 1b Logic connector // OR(0) AND(1)
    // 1b output polarity // if condition is true set
    // }
    // }
    // payload = [OutputStateConfig]
    public Map<Integer, byte[]> getEndpointOutputCfg(Registration registration) {
        Map<Integer, byte[]> output = new HashMap<Integer, byte[]>();
        OutputStateConfig c = new OutputStateConfig(OutputPolarity.HIGH, EventCode.ALARM, 1l,
                OutputTriggerType.EQUAL_OR_GREATER);
        output.put(0, c.toWriteByte());
        c = new OutputStateConfig(OutputPolarity.LOW, EventCode.ALARM, 1l, OutputTriggerType.EQUAL_OR_GREATER);
        output.put(1, c.toWriteByte());

        return output;
    }

    private void putLwM2mObjectInMemory(String endpoint, LwM2mObject obj) {
        if (this.mSimpleStorage != null) {
            String response = this.gson.toJson(obj);
            this.mSimpleStorage.setResource(endpoint, "/" + obj.getId() + "/", response);
        }
    }

    private LwM2mObject getLwM2mObjectFromMemory(String endpoint, int id) {
        LwM2mObject obj = null;
        if (this.mSimpleStorage != null) {
            String response = this.mSimpleStorage.getResource(endpoint, "/" + id + "/");
            if (response != null) {
                obj = this.gson.fromJson(response, LwM2mObject.class);
            }
        }
        return obj;
    }

    private void processData(LeshanServer server, Registration registration, String resourceLink, Payload payload,
            long timeout) {
        LwM2mNode object = Lwm2mHelper.readRequest(server, registration, resourceLink, timeout);
        if (object != null && object instanceof LwM2mObject) {
            LwM2mObject obj = (LwM2mObject) object;
            for (Map.Entry<Integer, LwM2mObjectInstance> entry : obj.getInstances().entrySet()) {
                processDataSub(payload, entry.getValue(), 0); // temperature
                processDataSub(payload, entry.getValue(), 1); // humidity
                processDataSub(payload, entry.getValue(), 2); // Pressure
                processDataSub(payload, entry.getValue(), 3); // CO2
                processDataSub(payload, entry.getValue(), 4); // CO
                processDataSub(payload, entry.getValue(), 5); // IAQ
            }
        }
    }

    private void processDataSub(Payload payload, LwM2mObjectInstance instance, int resource) {
        byte[] b = Lwm2mHelper.getOpaque(instance, resource);
        // byte header + data
        if (b != null && b.length > ByteUtil.CFG_HEADER_BYTES) {
            processSensorData(payload, instance.getId(), resource, b);
        }
    }

    // All bytes stored in LITTLE_ENDIAN byte decode
    // header = {
    // 4B unixtime first measurement time
    // 2B interval(measurement pulse)
    // 1B count(measurements in packet)
    // 1B {
    // 3b measurement byte size
    // 3b floating point(all data received in Integers and calculated)
    // 1b is repeat call(should check in last packet)
    // 1b reserved / not used
    // }
    // }
    // packet = header + data
    // payload = [packet,packet...]
    private void processSensorData(Payload payload, int instance, int resource, byte[] opaque) {
        // ->dbg
        StringBuilder sb = new StringBuilder();
        for(byte bs: opaque) {
            sb.append(ByteUtil.byteToString(bs));
            sb.append(" ");
        }
        LOG.debug("Instance:{};Resource:{};Opaque.length:{};Byte[]:{};",instance,resource,
            opaque.length, sb.toString());

        int posStart = 0; // each payload packet start position
        Map<Integer, Object> collect = null;
        int maxPacketCount = 5; // safety, do we need?
        int currentPacket = 0;
        while (posStart != opaque.length && currentPacket < maxPacketCount) {
            LOG.debug("Decoding Packet in while. Packet: {}; Array position: {};", currentPacket, posStart);
            currentPacket++;
            if(posStart + ByteUtil.CFG_HEADER_BYTES >= opaque.length) {
                LOG.error("Corrupted payload on instance: {}; resource: {}; opaque.length: {}; expected length: {};",
                instance, resource, opaque.length, posStart + ByteUtil.CFG_HEADER_BYTES);
            }
            PacketConfig cfg = new PacketConfig(Arrays.copyOfRange(opaque, posStart, posStart + ByteUtil.CFG_HEADER_BYTES)); 
            LOG.debug(cfg.toString());     
            payload.setIfIsRepeatCall(cfg.mIsRepeatCall);
            if (opaque.length >= posStart + cfg.mPacketByteSize && cfg.mMeasurementCount > 0) {
                if (collect == null) {
                    collect = new HashMap<Integer, Object>();
                }
                int intervalPosition = 0;
                byte[] byteArray = ByteUtil.getEmptyByteArray(0);
                for (int i = 0; i < (cfg.mMeasurementCount * cfg.mMeasurementByteCount); i++) {
                    int payloadPos = posStart + ByteUtil.CFG_HEADER_BYTES + i;
                    int valuePos = (i % cfg.mMeasurementByteCount);
                    byteArray[valuePos] = opaque[payloadPos];
                    // last byte in value array is added, create value
                    if (cfg.mOffset + valuePos == ByteUtil.VALUE_BYTES - 1) {
                        // ByteBuffer.wrap(rawValue).order(ByteOrder.LITTLE_ENDIAN).getInt();
                        int value = ByteUtil.byteToInt(byteArray);
                        int valueTim = cfg.mUnixTime + (intervalPosition * cfg.mInterval);
                        if (cfg.mPow < 0) {// @floatingPoint must be 0.1,0.01.. //double, round remove 12.2300000000001
                            collect.put(valueTim, ByteUtil.getDoubleRound((double) value * cfg.mPowValue, Math.abs(cfg.mPow)));
                        } else {// int @floatingPoint must be 1,10..
                            collect.put(valueTim, value * (int) cfg.mPowValue);
                        }
                        intervalPosition++;
                    }
                }
                posStart += cfg.mPacketByteSize;
            } else { //EXIT if something wrong with packet or only header provided
                posStart = opaque.length;
            }
        }
        if (collect != null) {
            payload.add(instance, resource, collect);
        }
    }

    private void sendAll(Map<String, ArrayList<String>> data) {
        if (this.mThingsboardSend != null) {
            for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
                if (entry.getValue().size() > 0) {
                    this.mThingsboardSend.send(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /** Scheduled executions */
    private void processScheduledOperations(Registration registration) {
        List<RequestPayload> requestPayload = this.mSimpleStorage.getEndpointRequests(registration.getEndpoint());
        if (requestPayload != null) {
            Collections.sort(requestPayload); // sorting asc
            for (RequestPayload item : requestPayload) {
                // execute only current / past, ignore scheduled executions in future
                if (item.mTimeMs <= System.currentTimeMillis()) {
                    ScheduleRequest request;
                    try {
                        request = this.gson.fromJson(item.mPayload, ScheduleRequest.class);
                        if (request.isValid()) {
                            boolean result = sendRequest(registration, request);
                            LOG.info("Processed request for {}; Link:{} isSuccess:{}; ReadResp:{};",
                                    registration.getEndpoint(), item.mLink, result, request.getReadResponse());
                            if (result && this.mEventServlet != null) {
                                if (result && request.isRead()) {
                                    mEventServlet.pushEvent(registration, request);
                                } // if write success, create LwM2mResponse to push event
                            }
                        } else {
                            LOG.error("Process is not valid for {}; Request:{}:{}:{};", registration.getEndpoint(),
                                    item.mLink, item.mPayload, item.mTimeMs);
                        }
                    } catch (Exception e) {
                        LOG.error("Process error for {}; Request:{}:{}:{};", registration.getEndpoint(), item.mLink,
                                item.mPayload, item.mTimeMs);
                    }
                    this.mSimpleStorage.deleteEndpointRequest(registration.getEndpoint(), item.mLink);
                } else {
                    LOG.debug("Process of {}:{} skipped for {};", item.mLink, item.mTimeMs, registration.getEndpoint());
                }
            }
        }
    }

    private boolean sendRequest(Registration registration, ScheduleRequest request) {
        try {
            if (request.isRead()) {
                LwM2mResponse obj = Lwm2mHelper.send(this.mLeshanServer, registration, request.getReadRequest(),
                        this.mTimeout);
                if (obj != null && obj instanceof ReadResponse) {
                    LwM2mNode node = ((ReadResponse) obj).getContent();
                    request.setReadResponse(node);
                    return true;
                }
            } else if (request.isWrite()) {
                LwM2mResponse obj = Lwm2mHelper.send(this.mLeshanServer, registration, request.getWriteRequest(),
                        this.mTimeout);
                return obj != null && obj.isSuccess();
            } else if (request.isExecute()) {
                LwM2mResponse obj = Lwm2mHelper.send(this.mLeshanServer, registration, request.getExecuteRequest(),
                        this.mTimeout);
                return obj != null && obj.isSuccess();
            } else if (request.isObserve()) {
                LwM2mResponse obj = Lwm2mHelper.send(this.mLeshanServer, registration, request.getObserveRequest(),
                        this.mTimeout);
                return obj != null && obj.isSuccess();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private final MyMessageTracer myMessageTracer = new MyMessageTracer();

    private final MessageInterceptorAdapter messageInterceptorAdapter = new MessageInterceptorAdapter() {
        @Override
        public void sendRequest(final Request request) {
            if(request.getNanoTimestamp() == 0) {
                request.addMessageObserver(new MessageObserver() {
                    @Override
                    public void onTimeout() {
                        LOG.warn("timeout at {} : {}", System.currentTimeMillis(), request);
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
    
                    @Override
                    public void onSent(boolean retransmission) {
                        LOG.warn("sent at {} : {} : {}", System.currentTimeMillis(), request);
                    }
                });
            }
            request.setNanoTimestamp(System.currentTimeMillis()); 
        }
    };
}