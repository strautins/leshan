package org.eclipse.leshan.server.demo.mt.scheduler;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

public class ScheduleRequestDeserializer implements JsonDeserializer<ScheduleRequest> {
    @Override
    public ScheduleRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json == null) {
            return null;
        }
        ScheduleRequest scheduleRequest;

        if (json.isJsonObject()) {
            JsonObject object = (JsonObject) json;

            String action = object.get(ScheduleRequestSerializer.JsonScheduleKeys.action.name()).getAsString();
            String path = object.get(ScheduleRequestSerializer.JsonScheduleKeys.path.name()).getAsString();
            LwM2mResource reqValues = null;
            if(object.has(ScheduleRequestSerializer.JsonScheduleKeys.resource_id.name())) {
                int resourceId = object.get(ScheduleRequestSerializer.JsonScheduleKeys.resource_id.name()).getAsInt();
                reqValues = getValues(resourceId, object, context,
                    ScheduleRequestSerializer.JsonScheduleKeys.resource_values.name(),
                    ScheduleRequestSerializer.JsonScheduleKeys.resource_value.name());
            }
           
            scheduleRequest = new ScheduleRequest(ScheduleRequest.getActionType(action), new LwM2mPath(path), reqValues);
            //has read response
            if (object.has(ScheduleRequestSerializer.JsonLwm2mKeys.id.name())) {
                LwM2mNode node;
                int id = object.get(ScheduleRequestSerializer.JsonLwm2mKeys.id.name()).getAsInt();
                if (object.has(ScheduleRequestSerializer.JsonLwm2mKeys.instances.name())) {

                    JsonArray array = object.get(ScheduleRequestSerializer.JsonLwm2mKeys.instances.name()).getAsJsonArray();
                    LwM2mObjectInstance[] instances = new LwM2mObjectInstance[array.size()];

                    for (int i = 0; i < array.size(); i++) {
                        instances[i] = context.deserialize(array.get(i), LwM2mNode.class);
                    }
                    node = new LwM2mObject(id, instances);

                } else if (object.has(ScheduleRequestSerializer.JsonLwm2mKeys.resources.name())) {
                    JsonArray array = object.get(ScheduleRequestSerializer.JsonLwm2mKeys.resources.name()).getAsJsonArray();
                    LwM2mResource[] resources = new LwM2mResource[array.size()];

                    for (int i = 0; i < array.size(); i++) {
                        resources[i] = context.deserialize(array.get(i), LwM2mNode.class);
                    }
                    node = new LwM2mObjectInstance(id, resources);

                } else if (object.has(ScheduleRequestSerializer.JsonLwm2mKeys.value.name()) ||
                    object.has(ScheduleRequestSerializer.JsonLwm2mKeys.values.name())) {
                        node =  getValues(id, object, context, 
                            ScheduleRequestSerializer.JsonLwm2mKeys.values.name(), 
                            ScheduleRequestSerializer.JsonLwm2mKeys.value.name());     
                } else {
                    throw new JsonParseException("Invalid node element");
                }
                scheduleRequest.setReadResponse(node);
            }
        } else {
            throw new JsonParseException("Invalid node element");
        }

        return scheduleRequest;
    }

    private org.eclipse.leshan.core.model.ResourceModel.Type getTypeFor(JsonPrimitive val) {
        if (val.isBoolean())
            return org.eclipse.leshan.core.model.ResourceModel.Type.BOOLEAN;
        if (val.isString())
            return org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
        if (val.isNumber()) {
            if (val.getAsDouble() == val.getAsLong()) {
                return org.eclipse.leshan.core.model.ResourceModel.Type.INTEGER;
            } else {
                return org.eclipse.leshan.core.model.ResourceModel.Type.FLOAT;
            }
        }
        // use string as default value
        return org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
    }

    private Object deserializeValue(JsonPrimitive val, org.eclipse.leshan.core.model.ResourceModel.Type expectedType) {
        switch (expectedType) {
        case BOOLEAN:
            return val.getAsBoolean();
        case STRING:
            return val.getAsString();
        case INTEGER:
            return val.getAsLong();
        case FLOAT:
            return val.getAsDouble();
        case TIME:
        case OPAQUE:
        default:
            // TODO we need to better handle this.
            return val.getAsString();
        }
    }
    
    private LwM2mResource getValues(int id, JsonObject object, JsonDeserializationContext context, String valuesName, String valueName) {
        if (object.has(valueName)) {
            // single value resource
            Object objVal;
            org.eclipse.leshan.core.model.ResourceModel.Type expectedType = null;
            JsonElement element = object.get(valueName);
            if(element.isJsonPrimitive()) {
                JsonPrimitive val = element.getAsJsonPrimitive();
                expectedType = getTypeFor(val);
                objVal = deserializeValue(val, expectedType);
            } else {
                objVal = getOpaque(context, element);
                expectedType = org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
            }
            return LwM2mSingleResource.newResource(id, objVal, expectedType);
        } else if (object.has(valuesName)) {
            // multi-instances resource
            Map<Integer, Object> values = new HashMap<>();
            org.eclipse.leshan.core.model.ResourceModel.Type expectedType = null;
            for (Entry<String, JsonElement> entry : object.get(valuesName).getAsJsonObject().entrySet()) {
                Object objVal;
                if(entry.getValue().isJsonPrimitive()) {
                    JsonPrimitive pval = entry.getValue().getAsJsonPrimitive();
                    expectedType = getTypeFor(pval);
                    objVal = deserializeValue(pval, expectedType);
                } else {
                    objVal = getOpaque(context, entry.getValue());
                    expectedType = org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
                }
                values.put(Integer.valueOf(entry.getKey()), objVal);
            }
            // use string by default;
            if (expectedType == null) {
                expectedType = org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
            }
            return LwM2mMultipleResource.newResource(id, values, expectedType);
        }
        return null;
    }
    
    private byte[] getOpaque(JsonDeserializationContext context, JsonElement element) {
        try {
            return Hex.decodeHex((char[]) context.deserialize(element,  char[].class));
        } catch (Exception e) {
            return null;
        }
    }
}
