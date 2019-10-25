package org.eclipse.leshan.server.demo.mt;

import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class RedisMessage {

    private static final Logger LOG = LoggerFactory.getLogger(OnConnectAction.class);
    
    private Pool<Jedis> mJedisPool = null;
    // Redis key prefixes
    private static final String EP_EVENT_LIST = "EP:EVENT:LIST"; /** global event list */
    private static final String PAYLOAD_EP = "PAYLOAD:EP:"; /** Endpoint => payload linkedlist */
    private static final String REQUEST_EP = "REQUEST:EP:"; /**Endpoint => request hashmap  */
    private static final String RESPONSE_EP = "RESPONSE:EP:"; /**Endpoint => response hashmap  */
    private static final String EP_INFO = "EP:INFO:"; /**key:value */
    
    public RedisMessage(Pool<Jedis> jedisPool) {
        this.mJedisPool = jedisPool;
    }

    public void sendPayload(Map<String, ArrayList<String>> DataMap) {
        try (Jedis j = mJedisPool.getResource()) {
            for (Map.Entry<String, ArrayList<String>> data : DataMap.entrySet()) {
                for (final String payload : data.getValue()) {
                    Long res = j.rpush(getEndpointPayloadKey(data.getKey()), payload);
                    if (res == null) {
                        LOG.warn("Redis rpush failed for {} : {}", getEndpointPayloadKey(data.getKey()), payload);
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
                    LOG.warn("Redis rpush failed for {} : {}", getEndpointPayloadKey(endpoint), payload);
                }
            }
        }
    }

    public void sendPayload(String endpoint, String payload) {
        try (Jedis j = mJedisPool.getResource()) {
            Long res = j.rpush(getEndpointPayloadKey(endpoint), payload);
            if (res == null) {
                LOG.warn("Redis rpush failed for {} : {}", getEndpointPayloadKey(endpoint), payload);
            }
        }
    }
    public void writeEventList(String endpoint) {
        try (Jedis j = mJedisPool.getResource()) {
            Long res = j.rpush(EP_EVENT_LIST, getEndpointPayloadKey(endpoint));
            if (res == null) {
                LOG.warn("Redis rpush failed for {} : {}", EP_EVENT_LIST, getEndpointPayloadKey(endpoint));
            }
        }
    }
    
    public void writeEventList(Map<Integer, String> resourceMap) {
        try (Jedis j = mJedisPool.getResource()) {
            for (Map.Entry<Integer, String> entry : resourceMap.entrySet()) {
                Long res = j.rpush(EP_EVENT_LIST, getEndpointPayloadKey(entry.getValue()));
                if (res == null) {
                    LOG.warn("Redis rpush failed for {} : {}", EP_EVENT_LIST, getEndpointPayloadKey(entry.getValue()));
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
    private String getEndpointInfoKey(String endpoint) {
        return EP_INFO + endpoint;
    }
    public Map<String, String> getEndpointRequests(String endpoint) {
        Map<String, String> payLoadMap = null;
        try (Jedis jedis = mJedisPool.getResource()) {
            payLoadMap = jedis.hgetAll(getEndpointRequestKey(endpoint));
        }
        return payLoadMap;
    }
    public String getEndpointInfo(String endpoint) {
        String payLoad = null;
        try (Jedis jedis = mJedisPool.getResource()) {
            payLoad = jedis.get(getEndpointInfoKey(endpoint));
        }
        return payLoad;
    }
    public void setEndpointInfo(String endpoint,  String payLoad) {
        try (Jedis jedis = mJedisPool.getResource()) {
            String s = jedis.set(getEndpointInfoKey(endpoint), payLoad);
            LOG.warn("Send info {}", s);
        }
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