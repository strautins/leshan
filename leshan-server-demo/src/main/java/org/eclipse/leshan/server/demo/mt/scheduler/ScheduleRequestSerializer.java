package org.eclipse.leshan.server.demo.mt.scheduler;

import java.lang.reflect.Type;
import java.util.Map.Entry;

import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.util.Hex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ScheduleRequestSerializer implements JsonSerializer<ScheduleRequest> {

    public static enum JsonScheduleKeys {
        action, path, resource_id, resource_values, resource_value
    }

    public static enum JsonLwm2mKeys {
        id, instances, resources, values, value
    }


    @Override
    public JsonElement serialize(ScheduleRequest src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject element = new JsonObject();
        element.addProperty(JsonScheduleKeys.action.name(), src.getAction().name());

        element.addProperty(JsonScheduleKeys.path.name(), src.getLwM2mPath().toString());
        //request value!
        if(src.getLwM2mResource() != null) {
            element.addProperty(JsonScheduleKeys.resource_id.name(), src.getLwM2mResource().getId());
            serializeResource(src.getLwM2mResource(), element, context, JsonScheduleKeys.resource_values.name(), JsonScheduleKeys.resource_value.name());
        }

        //serialize Write response
        if(src.getReadResponse() != null) {
            element.addProperty(JsonLwm2mKeys.id.name(), src.getReadResponse().getId());
            if (src.getReadResponse() instanceof LwM2mObject) {
                element.add(JsonLwm2mKeys.instances.name(), context.serialize(((LwM2mObject)src.getReadResponse()).getInstances().values()));
            } else if (src.getReadResponse() instanceof LwM2mObjectInstance) {
                element.add(JsonLwm2mKeys.resources.name(), context.serialize(((LwM2mObjectInstance)src.getReadResponse()).getResources().values()));
            } else if (src.getReadResponse() instanceof LwM2mResource) {
                LwM2mResource rsc = (LwM2mResource) src.getReadResponse();
                serializeResource(rsc, element, context, JsonLwm2mKeys.values.name(), JsonLwm2mKeys.value.name());
            }
        }
        return element;
    }
    
    private void serializeResource(LwM2mResource rsc, JsonObject element, JsonSerializationContext context, String valuesName, String valueName) {
        if (rsc.isMultiInstances()) {
            JsonObject values = new JsonObject();
            for (Entry<Integer, ?> entry : rsc.getValues().entrySet()) {
                if (rsc.getType() == org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE) {
                    values.add(entry.getKey().toString(),
                            context.serialize(Hex.encodeHex((byte[]) entry.getValue())));
                } else {
                    values.add(entry.getKey().toString(), context.serialize(entry.getValue()));
                }
            }
            element.add(valuesName, values);
        } else {
            if (rsc.getType() == org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE) {
                element.add(valueName, context.serialize(Hex.encodeHex((byte[]) rsc.getValue())));
            } else {
                element.add(valueName, context.serialize(rsc.getValue()));
            }
        }
    }
}
