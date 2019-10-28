package org.eclipse.leshan.server.demo.mt;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThingsboardMqttClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(OnConnectAction.class);
    
    private final String mHost;
    private final Integer mPort;
    private static final String mTopic = "v1/devices/me/telemetry";
    public ThingsboardMqttClient(String host, Integer port) throws URISyntaxException {
        LOG.warn("Created ThingsboardMqttClient at {} : {} : {}", System.currentTimeMillis(), host, port);
        this.mHost = host;
        this.mPort = port;
    }
    //Something not working :( org.fusesource.mqtt-client
    // public void connectAndPublish(final String token, final ArrayList<String> msg) throws URISyntaxException {
    //     MQTT mqtt = new MQTT();
    //     mqtt.setHost(this.mHost, this.mPort);
    //     mqtt.setUserName(token);
    //     mqtt.setCleanSession(true);
    //     mqtt.setConnectAttemptsMax(3);
    //     final CallbackConnection connection = mqtt.callbackConnection();
        
    //     LOG.warn("Mqtt before connect at {} : {} : {}", System.currentTimeMillis(), token, mqtt.getHost());
    //     connection.connect(new Callback<Void>() {
    //         public void onFailure(Throwable value) {
    //             LOG.warn("Mqtt Connection error at {} : {} : {}", System.currentTimeMillis(), token, value.getMessage());
    //             connection.disconnect(null);
    //         }
    //         public void onSuccess(Void v) {
    //             LOG.warn("Mqtt Connection success at {} : {} : {}", System.currentTimeMillis(), token);
    //             // Send a messages to a topic
    //             try {
    //                 final CountDownLatch latch = new CountDownLatch(msg.size());
    //                 for (final String s : msg) {
    //                     connection.publish(mTopic, s.getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
    //                         public void onSuccess(Void v) {
    //                             latch.countDown();
    //                             LOG.warn("Mqtt Received at {} : {} : {}", System.currentTimeMillis(), token, s);
    //                         }
    //                         public void onFailure(Throwable value) {
    //                             latch.countDown();
    //                             LOG.warn("Mqtt Failed at {} : {} : {} : {}", System.currentTimeMillis(), token, s, value);
    //                         }
    //                     });
    //                 }
    //                 latch.await();
    //             } catch (InterruptedException e) {
    //                 LOG.warn("Mqtt InterruptedException at {} : {} : {}", System.currentTimeMillis(), token, e.getMessage());
    //             } finally {
    //                 // To disconnect..
    //                 connection.disconnect(new Callback<Void>() {
    //                     public void onSuccess(Void v) {
    //                         LOG.warn("Mqtt disconnect success at {} : {}", System.currentTimeMillis(), token);
    //                     }
    //                     public void onFailure(Throwable value) {
    //                         // Disconnects never fail.
    //                         LOG.warn("Mqtt disconnect failed at {} : {}", System.currentTimeMillis(), value.getMessage());
    //                     }
    //                 });
    //             }
    //         }
    //     });
    //     LOG.warn("Mqtt End at {} : {} : {} : {}", System.currentTimeMillis(), token, this.mHost,  this.mPort);
    // }
    //org.eclipse.paho
    public void connectAndPublish(final String token, final ArrayList<String> msg) throws URISyntaxException {
        int qos = 2;
        String broker = "tcp://"+ this.mHost + ":" + this.mPort;
        MemoryPersistence persistence = new MemoryPersistence();

        try (MqttClient sampleClient = new MqttClient(broker, token.trim(), persistence)) {
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(token.trim());
            connOpts.setCleanSession(true);
            
            //LOG.warn("Mqtt connecting to broker {} : {}", broker, token.trim());
            sampleClient.connect(connOpts);
            //LOG.warn("Mqtt connected to broker {} : {}", broker, token.trim());
            for (final String s : msg) { 
                //LOG.warn("Mqtt publishing message for  {} : {}", token.trim(), s);
                MqttMessage message = new MqttMessage(s.getBytes());
                message.setQos(qos);
                sampleClient.publish(mTopic, message);
                //LOG.warn("Mqtt message published for {} : {}", token.trim(), s);
            }
            sampleClient.disconnect();
            //LOG.warn("Mqtt disconnected for {}", token.trim());
        } catch(MqttException me) {
            me.printStackTrace();
        }
    }
}