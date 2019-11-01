package org.eclipse.leshan.server.demo.mt;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.json.JSONArray;
import org.json.JSONObject;

public class EndpointCache {
    /** json serial mapping key */
    private static final String JSON_MAPPING_KEY = "mapping";
     /** json interval */ 
    private static final String JSON_INTERVAL_KEY = "interval";
    /** json timestamp */
    private static final String JSON_TIMESTAMP_KEY = "ts";
     /** json values */ 
    private static final String JSON_VALUES_KEY = "values";

    private final String endpoint;

    private final Map<Integer, String> serialMapping;
    private final Map<Integer, Map<Integer, Long>> sensorInterval;

    private final Map<String, Map<Long, JSONObject>> payloads;
    /**@endpointCache should not be null */
    public EndpointCache(String endpoint, String endpointCache) {
        JSONObject mainCache = new  JSONObject(endpointCache);
        this.serialMapping = new HashMap<Integer, String>();  
        this.sensorInterval = new HashMap<Integer, Map<Integer, Long>>();
        this.payloads = new HashMap<String, Map<Long, JSONObject>>();
        this.endpoint = endpoint;

        JSONObject SerialCache = mainCache.getJSONObject(JSON_MAPPING_KEY);
        Iterator<String> SerialKeys = SerialCache.keys();
        while (SerialKeys.hasNext()) {
            String SerialKey = SerialKeys.next();
            this.serialMapping.put(Integer.valueOf(SerialKey), SerialCache.getString(SerialKey));
        }

        JSONObject IntervalCache = mainCache.getJSONObject(JSON_INTERVAL_KEY);
        //while over instances
        Iterator<String> IntervalKeys = IntervalCache.keys();
        while (IntervalKeys.hasNext()) {
            Map<Integer, Long> intervalMap = new HashMap<Integer, Long>();
            String IntervalKey = IntervalKeys.next();
            JSONObject instance = IntervalCache.getJSONObject(IntervalKey);
            Iterator<String> instanceKeys = instance.keys();
            //while over sensor keys
            while (instanceKeys.hasNext()) {
                String instanceKey = instanceKeys.next();
                intervalMap.put(Integer.valueOf(instanceKey), instance.getLong(instanceKey));
            }
            sensorInterval.put(Integer.valueOf(IntervalKey), intervalMap);
        }
    }

    public EndpointCache(String endpoint, LwM2mNode devicesObject) {
        this.serialMapping = new HashMap<Integer, String>();  
        this.sensorInterval = new HashMap<Integer, Map<Integer, Long>>();
        this.payloads = new HashMap<String, Map<Long, JSONObject>>();
        this.endpoint = endpoint;

        for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject)devicesObject).getInstances().entrySet()) {
            //serial      
            serialMapping.put(entry.getKey(), OnConnectAction.getStringResource(entry.getValue(), OnConnectAction.RESOURCE_ID_SERIAL_NUMBER));
            //interval
            Map<Integer, Long> instanceInterval = new HashMap<Integer, Long>();
            Long value = OnConnectAction.getIntegerResource(entry.getValue(), OnConnectAction.RESOURCE_ID_TEMPERATURE_INTERVAL);       
            instanceInterval.put(OnConnectAction.OBJECT_ID_TEMPERATURE, value);
            value = OnConnectAction.getIntegerResource(entry.getValue(), OnConnectAction.RESOURCE_ID_HUMIDITY_INTERVAL);       
            instanceInterval.put(OnConnectAction.OBJECT_ID_HUMIDITY, value);
            value = OnConnectAction.getIntegerResource(entry.getValue(), OnConnectAction.RESOURCE_ID_PRESSURE_INTERVAL);    
            if(value != null) {
                instanceInterval.put(OnConnectAction.OBJECT_ID_PRESSURE, value);
            }   
            value = OnConnectAction.getIntegerResource(entry.getValue(), OnConnectAction.RESOURCE_ID_CO2_INTERVAL);     
            if(value != null) {  
                instanceInterval.put(OnConnectAction.OBJECT_ID_CO2, value);
            }
            value = OnConnectAction.getIntegerResource(entry.getValue(), OnConnectAction.RESOURCE_ID_CO_INTERVAL);       
            if(value != null) {
                instanceInterval.put(OnConnectAction.OBJECT_ID_CO, value);
            }
            sensorInterval.put(entry.getKey(), instanceInterval);
        }
    }
    public Long getIntervalKey(int instance, int key) {
        try {
            return sensorInterval.get(instance).get(key);        
        } catch (Exception e) {
            return null;
        }
    }
    public String getSerial(int instance) {
        try {
            return serialMapping.get(instance);        
        } catch (Exception e) {
            return null;
        }
    }
    public void setInterval(int instance, int resourceId, Long value) {
        int objectId = getObjectId(resourceId);
        sensorInterval.get(instance).put(objectId, value);
    }
    private int getObjectId(int resourceId) {
        int result = 0;
        switch (resourceId) {
        case OnConnectAction.RESOURCE_ID_TEMPERATURE_INTERVAL:
            result = OnConnectAction.OBJECT_ID_TEMPERATURE;
            break;
            case OnConnectAction.RESOURCE_ID_HUMIDITY_INTERVAL:
            result = OnConnectAction.OBJECT_ID_HUMIDITY;
            break;
            case OnConnectAction.RESOURCE_ID_PRESSURE_INTERVAL:
            result = OnConnectAction.OBJECT_ID_PRESSURE;
            break;
            case OnConnectAction.RESOURCE_ID_CO2_INTERVAL:
            result = OnConnectAction.OBJECT_ID_CO2;
            break;
            case OnConnectAction.RESOURCE_ID_CO_INTERVAL:
            result = OnConnectAction.OBJECT_ID_CO;
            break;
        default:
            result = 0;
        }
        return result;
    }
    public String toPayload() {
        JSONObject SerialCache = new JSONObject();
        for (Map.Entry<Integer, String> entry : serialMapping.entrySet()) {
            SerialCache.put(entry.getKey().toString(), entry.getValue());
        }
        
        JSONObject IntervalCache = new JSONObject();
        for (Map.Entry<Integer, Map<Integer, Long>> entry : sensorInterval.entrySet()) {
            JSONObject instJson = new JSONObject();
            for (Map.Entry<Integer, Long> subEntry : entry.getValue().entrySet()) {
                instJson.put(subEntry.getKey().toString(), subEntry.getValue());
            }
            IntervalCache.put(entry.getKey().toString(), instJson);
        }
        return new JSONObject().put(JSON_MAPPING_KEY, SerialCache).put(JSON_INTERVAL_KEY, IntervalCache).toString();
    }
    /**For payloads */
    public void createDevicesPayload(LwM2mNode devicesObject) {
        if (devicesObject != null && devicesObject instanceof LwM2mObject) {
            for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) devicesObject).getInstances()
                    .entrySet()) {
                String serialNr =  OnConnectAction.getStringResource(entry.getValue(), OnConnectAction.RESOURCE_ID_SERIAL_NUMBER);
                Boolean isReachable =  OnConnectAction.getBooleanResource(entry.getValue(), OnConnectAction.RESOURCE_ID_REACHABLE);
                Date lastActiveTime =  OnConnectAction.getDateResource(entry.getValue(), OnConnectAction.RESOURCE_ID_LAST_ACTIVE_TIME);
                Long battery =  OnConnectAction.getIntegerResource(entry.getValue(), OnConnectAction.RESOURCE_ID_BATTERY);
                Double battery_level =  OnConnectAction.getDoubleResource(entry.getValue(), OnConnectAction.RESOURCE_ID_BATTERY_LEVEL);
                long tim = System.currentTimeMillis();
                createPayload(serialNr, tim, OnConnectAction.NAME_REACHABLE, isReachable);
                createPayload(serialNr, tim, OnConnectAction.NAME_LATEST_TIME, lastActiveTime.getTime());
                createPayload(serialNr, tim, OnConnectAction.NAME_BATTERY, battery);
                createPayload(serialNr, tim, OnConnectAction.NAME_BATTERY_LEVEL, battery_level);
            }
        }
    }

    public void createAlarmPayload(LwM2mNode AlarmObject) {
        if (AlarmObject != null && AlarmObject instanceof LwM2mObject) {
            for (Map.Entry<Integer, LwM2mObjectInstance> entry : ((LwM2mObject) AlarmObject).getInstances()
                    .entrySet()) {
                String serialNr = getSerial(entry.getKey());
                Long smokeAlarm = OnConnectAction.getIntegerResource(entry.getValue(), OnConnectAction.RESOURCE_ID_SMOKE_ALARM);
                Boolean isHushed = OnConnectAction.getBooleanResource(entry.getValue(), OnConnectAction.RESOURCE_ID_HUSHED);
                Boolean isTemperatureAlarm = OnConnectAction.getBooleanResource(entry.getValue(), OnConnectAction.RESOURCE_ID_TEMPERATURE_ALARM);
                Boolean isCoAlarm = OnConnectAction.getBooleanResource(entry.getValue(), OnConnectAction.RESOURCE_ID_CO_ALARM);
                long tim = System.currentTimeMillis();
                createPayload(serialNr, tim, OnConnectAction.NAME_SMOKE_ALARM, smokeAlarm);
                createPayload(serialNr, tim, OnConnectAction.NAME_HUSHED, isHushed);
                createPayload(serialNr, tim, OnConnectAction.NAME_TEMPERATURE_ALARM, isTemperatureAlarm);
                createPayload(serialNr, tim, OnConnectAction.NAME_CO_ALARM, isCoAlarm);

                Double temperature = OnConnectAction.getDoubleResource(entry.getValue(), OnConnectAction.RESOURCE_ID_TEMPERATURE);
                Double pressure = OnConnectAction.getDoubleResource(entry.getValue(), OnConnectAction.RESOURCE_ID_PRESSURE);
                Long co2 = OnConnectAction.getIntegerResource(entry.getValue(), OnConnectAction.RESOURCE_ID_CO2);
                Long co = OnConnectAction.getIntegerResource(entry.getValue(), OnConnectAction.RESOURCE_ID_CO);
                Long humidity = OnConnectAction.getIntegerResource(entry.getValue(), OnConnectAction.RESOURCE_ID_HUMIDITY);

                createPayload(serialNr, tim, OnConnectAction.NAME_ALARM_TEMPERATURE, temperature);
                createPayload(serialNr, tim, OnConnectAction.NAME_ALARM_ATMOSPHERIC, pressure);
                createPayload(serialNr, tim, OnConnectAction.NAME_ALARM_CO2, co2);
                createPayload(serialNr, tim, OnConnectAction.NAME_ALARM_CO, co);
                createPayload(serialNr, tim, OnConnectAction.NAME_ALARM_HUMIDITY, humidity);
            }
        }
    }
    private void createPayload(String serialNumber, Long tim, String resName, Object value) {
        // Serial payloads
        Map<Long, JSONObject> payloadMap = this.payloads.get(serialNumber);
        if (payloadMap == null) {
            payloadMap = new HashMap<Long, JSONObject>();
            this.payloads.put(serialNumber, payloadMap);
        }
        // tim payloads
        JSONObject payloadObj = payloadMap.get(tim);
        if (payloadObj == null) {
            payloadObj = new JSONObject("{\"" + resName + "\":" + value + "}");
            payloadMap.put(tim, payloadObj);
        } else {
            payloadObj.put(resName, value);
        }
    }
    public void createPayload(String serialNumber, Date lmt, Map<Integer, Object> resourceMap, String resName, Long pulse) {
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
    public Map<String, ArrayList<String>> serializePayloadAll() {
        Map<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
        for (Map.Entry<String, Map<Long, JSONObject>> entry : payloads.entrySet()) {
            String payload = serializePayloads(entry.getValue());
            ArrayList<String> payloadList = new ArrayList<String>();
            payloadList.add(payload);
            result.put(entry.getKey(), payloadList);
        }
        return result;
    }
    private String serializePayloads(Map<Long, JSONObject> data) {
        JSONArray array = new JSONArray();
        for (Map.Entry<Long, JSONObject> entry : data.entrySet()) {
            JSONObject superObj = new JSONObject();
            superObj.put(JSON_TIMESTAMP_KEY, entry.getKey());
            superObj.put(JSON_VALUES_KEY, entry.getValue());
            array.put(superObj);
        }
        return array.toString();
    }
    public Boolean lock() {
        // Striped<Lock> locks = Striped.lock(stripes);
        // Lock l = locks.get(string);
        // l.lock();
        // try {
        // // do stuff 
        // } finally {
        //     l.unlock();
        // }
        return false;
    }
    public Boolean unlock() {
        return false;
    }
}