package org.eclipse.leshan.server.demo.mt;

import java.io.IOException;
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

import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
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
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;

public class OnConnectAction {

    private static final Logger LOG = LoggerFactory.getLogger(OnConnectAction.class);

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private final LeshanServer mLeshanServer;
    private final ThingsboardMqttClient mThingsboardMqttClient;
    private final ThingsboardHttpClient mThingsboardHttpClient;
    private final RedisMessage mRedisMessage;
    private final long mTimeout;

    private final Gson gson;

    public OnConnectAction(LeshanServer leshanServer, ThingsboardMqttClient lwM2mMqttClient, ThingsboardHttpClient thingsboardHttpClient, RedisMessage redisMessage)
            throws URISyntaxException {
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
            getRecourses(registration, EVENT_REGISTRATION, registration.getEndpoint());
        }

        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                Registration previousRegistration) {
            getRecourses(updatedRegistration, EVENT_UPDATED, updatedRegistration.getEndpoint());
        }

        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                Registration newReg) {
            getRecourses(registration, EVENT_DEREGISTRATION, registration.getEndpoint());
        }

    };

    private final ObservationListener observationListener = new ObservationListener() {
        @Override
        public void newObservation(Observation observation, Registration registration) {
            LOG.warn("NEW Observation () for  {}", observation.getPath().toString(), registration.getEndpoint());  
        }

        @Override
        public void cancelled(Observation observation) {
            LOG.warn("Observation Canceled {}", observation.getPath().toString());
        }

        @Override
        public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
            if (registration != null && observation.getPath().toString().equals("/43000")) {
                try {
                    readObject(registration, observation, "43005");
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
        this.mLeshanServer.coap().getUnsecuredEndpoint().addInterceptor(this.messageInterceptorAdapter);
    }

    public void stop() {
        this.mLeshanServer.getRegistrationService().removeListener(this.registrationListener);
        this.mLeshanServer.getObservationService().removeListener(this.observationListener);
        this.mLeshanServer.coap().getUnsecuredEndpoint().removeInterceptor(this.messageInterceptorAdapter);
    }

    private void readObject(Registration registration, Observation observation, String objLink) throws InterruptedException {
        ReadResponse readRes = this.mLeshanServer.send(registration, new ReadRequest(objLink), this.mTimeout);
        if (readRes == null) {
            LOG.warn("Read object {} timeout", objLink);
        } else if (readRes.isSuccess()) {
            LOG.warn("Read object {} Success. {}", objLink, readRes.getContent());
        } else {
            LOG.warn("Read object {} failed. {}", objLink, readRes.getErrorMessage());
        }
        int i = mLeshanServer.getObservationService().cancelObservations(registration,
                observation.getPath().toString());
        LOG.warn("Observation {} canceled = {}",observation.getPath().toString(),  i);
    }

    private void getRecourses(Registration registration, String event, String endpoint) {
        LOG.warn("Online {} with {}",  registration.getEndpoint(), event);
        if (event.equals(EVENT_REGISTRATION) || event.equals(EVENT_UPDATED)) {
            if (registration.getObjectLinks() != null) {
                boolean isMainObj = false;
                boolean isHumidity = false;
                boolean isCo2 = false;
                boolean isTemperature = false;
                boolean isCo = false;
                boolean isAtmospheric = false;
                boolean isAlertStatus = false;
                for (Link i : registration.getObjectLinks()) {
                    if (i.getUrl().contains("/43000/0")) {
                        isMainObj = true;
                    } else if (i.getUrl().contains("/43002/")) {
                        isTemperature = true;
                    } else if (i.getUrl().contains("/43003/")) {
                        isHumidity = true;
                    } else if (i.getUrl().contains("/43006/")) {
                        isCo2 = true;
                    } else if (i.getUrl().contains("/43007/")) {
                        isCo = true;
                    } else if (i.getUrl().contains("/43004/")) {
                        isAtmospheric = true;
                    } else if (i.getUrl().contains("/43005/")) {
                        isAlertStatus = true;
                    }
                }
                if (isMainObj) {
                    try {
                        ReadResponse response = this.mLeshanServer.send(registration, new ReadRequest(43000),
                                this.mTimeout);
                        if (response == null) {
                            LOG.warn("Main object call timeout for {}",  registration.getEndpoint());
                        } else if (response.isSuccess()) {
                            LwM2mObjectInstance inst = ((LwM2mObject) response.getContent()).getInstance(0);
                            Map<Integer, LwM2mResource> resources = inst.getResources();
                            LwM2mMultipleResource resource = (LwM2mMultipleResource) resources.get(1);
                            if (resource.getType().equals(Type.STRING)) {
                                @SuppressWarnings("unchecked")
                                Map<Integer, String> resourceMap = (Map<Integer, String>) resource.getValues();
                                Map<String, Map<Long, JSONObject>> payloads = new HashMap<String, Map<Long, JSONObject>>();
                                if (isTemperature) {
                                    processData(registration, resourceMap, 43002, "temperature", payloads);
                                }
                                if (isHumidity) {
                                    processData(registration, resourceMap, 43003, "humidity", payloads);
                                }
                                if (isCo2) {
                                    processData(registration, resourceMap, 43006, "co2", payloads);
                                }
                                if (isCo) {
                                    processData(registration, resourceMap, 43007, "co", payloads);
                                }
                                if (isAtmospheric) {
                                    processData(registration, resourceMap, 43004, "atmospheric", payloads);
                                }
                                if (isAlertStatus) {
                                    observeRequest(registration, "43000");
                                }
                                Map<String, ArrayList<String>> result = serializePayloadAll(payloads);
                                sendAll(result);
                                //collecting request from redis
                                if(this.mRedisMessage != null) {
                                    processRedisRequests(registration, resourceMap);
                                }
                            } else {
                                LOG.warn("Main object {} datatype error {}",  registration.getEndpoint(), resource.getType());
                            }
                        } else {
                            LOG.warn("Failed to read Main object for {} Error code {} Error Message {}",  registration.getEndpoint(), response.getCode(), response.getErrorMessage());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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
                LOG.warn("ObserveRequest for {} on {} timeout!",  registration.getEndpoint(), link);
            } else if (response.isSuccess()) {
                LOG.warn("ObserveRequest for {} on {} success! : {}",  registration.getEndpoint(), link, response.getContent());
            } else {
                LOG.warn("ObserveRequest for {} on {} Failed! Error : {} : {}",  registration.getEndpoint(), link, response.getCode(), response.getErrorMessage());
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
                send2HttpApi(entry.getValue(), entry.getKey());
            }
        } 
        if (this.mThingsboardMqttClient != null) {
            for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
                send2Mqtt(entry.getValue(), entry.getKey());
            }
        } 
        if (this.mRedisMessage != null) {
            this.mRedisMessage.sendPayload(data);
            for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
                this.mRedisMessage.writeEventList(entry.getKey());
            }
        }
    }

    private void send2Mqtt(ArrayList<String> payloadArray, String token) {
        try {
            this.mThingsboardMqttClient.connectAndPublish2(token, payloadArray);
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

    private void processData(Registration registration, Map<Integer, String> resourceMap, int objId, String resName,
            Map<String, Map<Long, JSONObject>> payloads) {
        try {
            LOG.warn("ReadRequest for {} on object {}!",  registration.getEndpoint(), objId);
            ReadResponse response = this.mLeshanServer.send(registration, new ReadRequest(objId), this.mTimeout);
            // set read values
            if (response == null) {
                LOG.warn("ReadRequest for {} on object {} timeout!",  registration.getEndpoint(), objId);
            } else if (response.isSuccess()) {
                // if we got object
                LwM2mObject obj = ((LwM2mObject) response.getContent());
                // for over instance/ serials
                for (Map.Entry<Integer, String> entry : resourceMap.entrySet()) {
                    LwM2mObjectInstance inst = obj.getInstance(entry.getKey());
                    Long pulse = getIntegerResource(inst, 1);
                    Map<Integer, Object> resMap = getResourceMap(inst, 2);
                    Date lmt = getDateResource(inst, 3);
                    if (lmt != null && resMap != null && pulse != null && resMap.size() > 0) {
                        Long defTime = lmt.getTime();
                        Map<Long, JSONObject> payloadMap = payloads.get(entry.getValue());
                        if (payloadMap == null) {
                            payloadMap = new HashMap<Long, JSONObject>();
                            payloads.put(entry.getValue(), payloadMap);
                        }
                        for (Map.Entry<Integer, Object> resEntry : resMap.entrySet()) {
                            Long calcTime = (defTime - (((resMap.size() - 1) - resEntry.getKey()) * 1000 * pulse));
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
                }
                clearObject(registration, resourceMap, obj);
            } else {
                LOG.warn("ReadRequest for {} on object {} Failed! Error: {} : {}",  registration.getEndpoint(), objId, response.getCode(), response.getErrorMessage());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void clearObject(Registration registration, Map<Integer, String> resourceMap, LwM2mObject obj) {
        for (Map.Entry<Integer, String> entry : resourceMap.entrySet()) {
            LwM2mObjectInstance inst = obj.getInstance(entry.getKey());
            Date lmt = getDateResource(inst, 3);
            Date lrmt = getDateResource(inst, 4);
            if (lmt != null && (lrmt == null || lrmt != null && lmt.getTime() >= lrmt.getTime())) {
                WriteRequest request = new WriteRequest(WriteRequest.Mode.UPDATE, obj.getId(), entry.getKey(),
                        LwM2mSingleResource.newDateResource(4, lmt));
                // final String clearingObj = obj.getId() + "/" + entry.getKey();
                // System.out.println("Clearing obj/instance: " + clearingObj);
                // this.mLeshanServer.send(registration, request, this.mTimeout,
                // new ResponseCallback<WriteResponse>() {
                // @Override
                // public void onResponse(WriteResponse response) {
                // if(!response.isSuccess()) {
                // System.out.println("ResponseCallback Something wrong = " + clearingObj + "/"
                // + response.getErrorMessage());
                // } else {
                // System.out.println("ResponseCallback isSuccess " + clearingObj + "/" +
                // response.getCoapResponse().toString());
                // }
                // }
                // },
                // new ErrorCallback() {
                // @Override
                // public void onError(Exception e) {
                // System.out.println("ErrorCallback = " + clearingObj + "/" + e.getMessage());
                // }
                // }
                // );
                try {
                    LwM2mResponse response = this.mLeshanServer.send(registration, request, this.mTimeout);
                    if (response == null) {
                        LOG.warn("WriteRequest for {} on object {} timeout!",  registration.getEndpoint(), request.getPath().toString());
                    } else if (!response.isSuccess()) {
                        LOG.warn("WriteRequest for {} on object {} Failed! Error: {} : {}",  registration.getEndpoint(), request.getPath().toString(), response.getCode(), response.getErrorMessage());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                LOG.warn("Clear object skipped for {} on object {}/{} due to unfulfilled conditions!",  registration.getEndpoint(), obj, entry.getKey());
            }
        }
    }

    private Date getDateResource(LwM2mObjectInstance instObj, int res) {
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.TIME)) {
                return (Date) lmtRes.getValue();
            }
        }
        return null;
    }

    private Long getIntegerResource(LwM2mObjectInstance instObj, int res) {
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.INTEGER)) {
                return (Long) lmtRes.getValue();
            }
        }
        return null;
    }

    private Map<Integer, Object> getResourceMap(LwM2mObjectInstance instObj, int inst) {
        Map<Integer, LwM2mResource> resources = instObj.getResources();
        LwM2mMultipleResource resource = (LwM2mMultipleResource) resources.get(inst);
        @SuppressWarnings("unchecked")
        Map<Integer, Object> resourceMap = (Map<Integer, Object>) resource.getValues();
        return resourceMap;
    }

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