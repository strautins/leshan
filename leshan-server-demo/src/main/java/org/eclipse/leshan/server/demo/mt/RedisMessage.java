package org.eclipse.leshan.server.demo.mt;

import java.util.ArrayList;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class RedisMessage {

    private Pool<Jedis> mJedisPool = null;
    // Redis key prefixes
    private static final String EP_EVENT_LIST = "EP:EVENT:LIST"; // global event list
    private static final String PAYLOAD_EP = "PAYLOAD:EP:"; // (Endpoint => payload linkedlist)
    private static final String REQUEST_EP = "REQUEST:EP:"; // (Endpoint => request hashmap)
    private static final String RESPONSE_EP = "RESPONSE:EP:"; // (Endpoint => response hashmap)
    
    public RedisMessage(Pool<Jedis> jedisPool) {
        this.mJedisPool = jedisPool;
    }

    public void sendPayload(Map<String, ArrayList<String>> DataMap) {
        try (Jedis j = mJedisPool.getResource()) {
            for (Map.Entry<String, ArrayList<String>> data : DataMap.entrySet()) {
                for (final String payload : data.getValue()) {
                    Long res = j.rpush(getEndpointPayloadKey(data.getKey()), payload);
                    if (res == null) {
                        System.err.println("payload send Error =" + getEndpointPayloadKey(data.getKey()));
                    }
                }
            } 
        }
    }

    public void sendPayload(String endpoint, ArrayList<String> Data) {
        try (Jedis j = mJedisPool.getResource()) {
            for (final String payload : Data) {
                Long res = j.rpush(getEndpointPayloadKey(endpoint), payload);
                if (res == null) {
                    System.err.println("payload send Error =" + getEndpointPayloadKey(endpoint));
                }
            }
        }
    }

    public void sendPayload(String endpoint, String payload) {
        try (Jedis j = mJedisPool.getResource()) {
            Long res = j.rpush(getEndpointPayloadKey(endpoint), payload);
            if (res == null) {
                System.err.println("payload send Error =" + getEndpointPayloadKey(endpoint));
            }
        }
    }

    public void writeEventList(String endpoint) {
        try (Jedis j = mJedisPool.getResource()) {
            Long res = j.rpush(EP_EVENT_LIST, getEndpointPayloadKey(endpoint));
            if (res == null) {
                System.err.println("Write error =" + getEndpointPayloadKey(endpoint));
            }
        }
    }
    
    public void writeEventList(Map<Integer, String> resourceMap) {
        try (Jedis j = mJedisPool.getResource()) {
            for (Map.Entry<Integer, String> entry : resourceMap.entrySet()) {
                Long res = j.rpush(EP_EVENT_LIST, getEndpointPayloadKey(entry.getValue()));
                if (res == null) {
                    System.err.println("Write error =" + getEndpointPayloadKey(entry.getValue()));
                }
            }
        }
    }
    private String getEndpointPayloadKey(String endpoint) {
        return PAYLOAD_EP + endpoint;
    }
    private String getEndpointRequestKey(String endpoint) {
        return REQUEST_EP + endpoint;
    }
    private String getEndpointResponseKey(String endpoint) {
        return RESPONSE_EP + endpoint;
    }
    public Map<String, String> getEndpointRequests(String endpoint) {
        Map<String, String> payLoadMap = null;
        try (Jedis jedis = mJedisPool.getResource()) {
            payLoadMap = jedis.hgetAll(getEndpointRequestKey(endpoint));
        }
        return payLoadMap;
    }
    public void sendResponse(String endpoint, Map<String, String> responseList) {
        try (Jedis jedis = mJedisPool.getResource()) {
            for (Map.Entry<String, String> entry : responseList.entrySet()) {
                Long res = jedis.hdel(getEndpointRequestKey(endpoint), entry.getKey());
                //request still actual, add in processed list
                if(res == 1) {
                    jedis.hset(getEndpointResponseKey(endpoint), entry.getKey(), entry.getValue());       
                }
            }
        }
    }
}