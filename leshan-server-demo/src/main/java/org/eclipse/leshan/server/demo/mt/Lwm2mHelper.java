package org.eclipse.leshan.server.demo.mt;

import java.util.Date;
import java.util.Map;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lwm2mHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(Lwm2mHelper.class);

    public static LwM2mNode readRequest(LeshanServer server, Registration registration, String resourceLink, long timeout) {
        LwM2mNode object = null;
        try {
            LOG.debug("Sending read to {} on {} at {}", registration.getEndpoint(), resourceLink,
                    System.currentTimeMillis());
            ReadResponse response = server.send(registration, new ReadRequest(resourceLink), timeout);
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

    public static Boolean writeRequest(LeshanServer server, Registration registration, WriteRequest request, long timeout) {
        Boolean result = null;
        String resourceInfo = null;
        if(request.getNode() instanceof LwM2mObjectInstance) {
            resourceInfo = ((LwM2mObjectInstance)request.getNode()).getResources().toString();
        }
        try {
            LOG.debug("Sending write to {} on {}({}) at {}", registration.getEndpoint(), request.getPath().toString(), resourceInfo,
                    System.currentTimeMillis());
            LwM2mResponse response = server.send(registration, request, timeout);
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

    public static void createExecuteRequest(LeshanServer server, Registration registration, String  path, String parameters, long timeout) {
        ExecuteRequest request = new ExecuteRequest(path, parameters);
        final String debug = registration.getEndpoint() + "; on" + request.getPath().toString();
        LOG.debug("ExecuteRequest for {} on {} at {}", registration.getEndpoint(),
                request.getPath().toString(), System.currentTimeMillis());

        server.send(registration, request, timeout, new ResponseCallback<ExecuteResponse>() {
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
    
    public static void observeRequest(LeshanServer server, Registration registration, String link, long timeout) {
        try {
            //// leshan client WriteAttributesRequest failed:INTERNAL_SERVER_ERROR not
            //// implemented
            // AttributeSet attributes = AttributeSet.parse("pmin=10&pmax=30");
            // WriteAttributesRequest write = new WriteAttributesRequest(OBJECT_ID_GROUP, attributes);
            // WriteAttributesResponse cResponse = this.mLeshanServer.send(registration,
            //// write, this.mTimeout);
            // if(cResponse.isSuccess()) {
            ReadResponse response = server.send(registration, new ObserveRequest(link), timeout);
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

    public static byte[] readOpaque(LeshanServer server, Registration registration, LwM2mPath resourceLink, long timeout) {
        byte[] value = null;
        LwM2mNode object = readRequest(server, registration, resourceLink.toString(), timeout);
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

    public static boolean writeOpaque(LeshanServer server, Registration registration, LwM2mPath resourceLink, byte[] byteValue, long timeout) {
        WriteRequest r = new WriteRequest(resourceLink.getObjectId(), 
            resourceLink.getObjectInstanceId(), 
            resourceLink.getResourceId(), byteValue);
        return writeRequest(server, registration, r, timeout);
    }

    public static boolean writeOpaque(LeshanServer server, Registration registration, LwM2mPath resourceLink,  Map<Integer, byte[]> map, long timeout) {
        WriteRequest r = new WriteRequest(resourceLink.getObjectId(), 
            resourceLink.getObjectInstanceId(), 
            resourceLink.getResourceId(), map, 
            ResourceModel.Type.OPAQUE);
        return writeRequest(server, registration, r, timeout);
    }

    public static byte[] getOpaque(LwM2mObjectInstance instObj, int res) {
        LwM2mResource resource = instObj.getResources().get(res);
        if(resource != null && resource instanceof LwM2mSingleResource) {
            //@SuppressWarnings("unchecked")
            byte[] resourceMap = (byte[])((LwM2mSingleResource)resource).getValue();
            return resourceMap;
        }
        return null;
    }

    public static Boolean getBooleanResource(LwM2mObjectInstance instObj, int res) {
        Boolean result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.BOOLEAN)) {
                result = (Boolean) lmtRes.getValue();
            }
        }
        return result;
    }

    public static String getStringResource(LwM2mObjectInstance instObj, int res) {
        String result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.STRING)) {
                result = (String) lmtRes.getValue();
            }
        }
        return result;
    }

    public static Date getDateResource(LwM2mObjectInstance instObj, int res) {
        Date result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.TIME)) {
                result = (Date) lmtRes.getValue();
            }
        }
        return result;
    }

    public static Long getIntegerResource(LwM2mObjectInstance instObj, int res) {
        Long result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.INTEGER)) {
                result = (Long) lmtRes.getValue();
            }
        }
        return result;
    }
    
    public static Double getDoubleResource(LwM2mObjectInstance instObj, int res) {
        Double result = null;
        if (instObj.getResource(res) instanceof LwM2mSingleResource) {
            LwM2mSingleResource lmtRes = (LwM2mSingleResource) instObj.getResource(res);
            if (lmtRes.getType().equals(Type.FLOAT)) {
                result = (Double) lmtRes.getValue();
            }
        }
        return result;
    }

    public static Map<Integer, Object> getResourceMap(LwM2mObjectInstance instObj, int res) {
        LwM2mResource resource = instObj.getResources().get(res);
        if(resource != null && resource instanceof LwM2mMultipleResource && resource.isMultiInstances()) {
            @SuppressWarnings("unchecked")
            Map<Integer, Object> resourceMap = (Map<Integer, Object>) ((LwM2mMultipleResource)resource).getValues();
            return resourceMap;
        }
        return null;
    }
}