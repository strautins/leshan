package org.eclipse.leshan.server.demo.mt;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Payload {
    
    private static final Logger LOG = LoggerFactory.getLogger(Payload.class);

    private static final String NAME_TEMPERATURE = "temperature";
    private static final String NAME_HUMIDITY = "humidity";
    private static final String NAME_CO2 = "co2";
    private static final String NAME_CO = "co";
    private static final String NAME_PRESSURE = "atmospheric";
    private static final String NAME_NONE = "none"; 

    private static final String TIMESTAMP_KEY = "ts";
    private static final String VALUES_KEY = "values";

    private boolean mRepeatCall = true;
    //<instance:<resource:<unixTime:value>>>
    private Map<Integer, Map<Integer,  Map<Integer, Object>>> mCollections;

    public void init() {
        this.mCollections = new HashMap<Integer, Map<Integer,  Map<Integer, Object>>>();
        this.mRepeatCall = false;
    }

    public void add(int instance, int resource, Map<Integer, Object> values) {
        Map<Integer,  Map<Integer, Object>> instResources = mCollections.get(instance);
        if(instResources == null) {
            instResources = new HashMap<Integer, Map<Integer, Object>>();
            mCollections.put(instance, instResources);
        }    
        instResources.put(resource, values);
    }
    public void setIfIsRepeatCall(boolean value) {
        mRepeatCall = value ? value : mRepeatCall;
    }

    public boolean isRepeatCall() {
        return mRepeatCall;
    }

    public boolean isData() {
        return !mCollections.isEmpty();
    }
    
    public String getPayload(int instance) {
        //resource:<time:value>
        Map<Integer,  Map<Integer, Object>> instMap = mCollections.get(instance);
        if(instMap != null) {
            //time:<resource,value>
            Map<Integer,  Map<Integer, Object>> serialize = new HashMap<Integer, Map<Integer, Object>>();
            //resource:<time:value>
            for(Map.Entry<Integer,  Map<Integer, Object>> entry : instMap.entrySet()) {
                //time:value
                //LOG.error("===>{}:{}", entry.getKey(), entry.getValue());
                for(Map.Entry<Integer, Object> res : entry.getValue().entrySet()) {
                    //LOG.error("==================>{}:{}", res.getKey(), res.getValue());
                    //resource,value
                    Map<Integer, Object> resInTime = serialize.get(res.getKey());
                    if(resInTime == null) {
                        serialize.put(res.getKey(), resInTime = new HashMap<Integer, Object>());   
                    }
                    resInTime.put(entry.getKey(), res.getValue());
                }
            }
            //prepare payload 
            //time:<resource:value>
            StringBuilder payload = new StringBuilder();
            payload.append("[");
            String arrPrefix = "";
            for(Map.Entry<Integer,  Map<Integer, Object>> entry : serialize.entrySet()) {
                payload.append(arrPrefix);
                arrPrefix = ",";
                payload.append("{");
                payload.append(wrap(TIMESTAMP_KEY));
                payload.append(":");
                payload.append(((long)entry.getKey()) * 1000);
                payload.append(",");
                payload.append(wrap(VALUES_KEY));
                payload.append(":{");
                //resource:value
                String prefix = "";
                for(Map.Entry<Integer, Object> res : entry.getValue().entrySet()) {
                    /**String payload = "{\"ts\":" + calcTime +",\"values\":{\"temperature\":" 
                     *  + resEntry.getValue() + ", \"co2\":"+ co2 +"}}"; 
                     */ 
                    String resName = getSensorName(res.getKey());
                    payload.append(prefix);
                    prefix = ",";
                    payload.append(wrap(resName));
                    payload.append(":");
                    payload.append(res.getValue());
                }
                payload.append("}}");
            }
            payload.append("]");
            //LOG.error("payload:{}", payload.toString());
            return payload.toString();
        }
        return null;
    }
    private static String getSensorName(int resource) {
        String result = null;
        switch (resource) {
        case 0:
            result = NAME_TEMPERATURE;
            break;
        case 1:
            result = NAME_HUMIDITY;
            break;
        case 2:
            result = NAME_PRESSURE;
            break;
        case 3:
            result = NAME_CO2;
            break;
        case 4:
            result = NAME_CO;
            break;
        default:
            result = NAME_NONE;
        }
        return result;
    }
    private static String wrap(String value) {
		return "\"" + value.replace("\"", "") +  "\"";
    }
}