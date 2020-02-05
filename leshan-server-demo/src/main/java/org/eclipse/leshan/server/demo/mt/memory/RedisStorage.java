package org.eclipse.leshan.server.demo.mt.memory;

import java.util.Map;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class RedisStorage implements SimpleStorage {

    //private static final Logger LOG = LoggerFactory.getLogger(RedisStorage.class);
    
    private Pool<Jedis> mJedisPool = null;
    // Redis key prefixes
    private static final String REQUEST_EP = "REQUEST:EP:"; /**Endpoint => request hashmap  */
    private static final String RESPONSE_EP = "RESPONSE:EP:"; /**Endpoint => response hashmap  */
    private static final String EP_RESOURCE = "EP:RESOURCE:"; /** key:value */

    private static final String NX_OPTION = "NX"; // set the key if it does not already exist
    private static final String PX_OPTION = "PX"; // expire time in millisecond
    
    public RedisStorage(Pool<Jedis> jedisPool) {
        this.mJedisPool = jedisPool;
    }

    private String getKey(String key, String endpoint) {
        return key + endpoint;
    }

    @Override
    public String getResource(String endpoint, String resourceLink) {
        try (Jedis jedis = mJedisPool.getResource()) {
            resourceLink = resourceLink.replace("/", ":");
            return jedis.hget(getKey(EP_RESOURCE, endpoint), resourceLink);  
        }
    }

    @Override
    public void setResource(String endpoint, String resourceLink,  String resource) {
        try (Jedis jedis = mJedisPool.getResource()) {
            resourceLink = resourceLink.replace("/", ":");
            jedis.hset(getKey(EP_RESOURCE, endpoint), resourceLink, resource);      
        }
    }
    
    @Override
    public void deleteEndpointData(String endpoint) {
        try (Jedis jedis = mJedisPool.getResource()) {
            jedis.del(getKey(EP_RESOURCE, endpoint));      
            jedis.del(getKey(REQUEST_EP, endpoint));   
            jedis.del(getKey(RESPONSE_EP, endpoint));   
        }
    }

    public Map<String, String> getEndpointRequests(String endpoint) {
        Map<String, String> payLoadMap = null;
        try (Jedis jedis = mJedisPool.getResource()) {
            payLoadMap = jedis.hgetAll(getKey(REQUEST_EP, endpoint));
        }
        return payLoadMap;
    }

    public void sendResponse(String endpoint, Map<String, String> responseList) {
        try (Jedis jedis = mJedisPool.getResource()) {
            for (Map.Entry<String, String> entry : responseList.entrySet()) {
                Long res = jedis.hdel(getKey(REQUEST_EP, endpoint), entry.getKey());
                //request still actual, add in processed list
                if(res == 1) {
                    jedis.hset(getKey(RESPONSE_EP, endpoint), entry.getKey(), entry.getValue());       
                }
            }
        }
    }
}