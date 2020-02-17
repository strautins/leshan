package org.eclipse.leshan.server.demo.mt.tb;

import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThingsboardMqttClient implements ThingsboardSend {

    private static final Logger LOG = LoggerFactory.getLogger(ThingsboardMqttClient.class);
    private final String HOST;
    private final Integer PORT;
    private final Integer TIMEOUT;
    private static final String mTopic = "v1/devices/me/telemetry";

    public ThingsboardMqttClient(String host, Integer port, int timeoutSec) throws URISyntaxException {
        LOG.warn("Created ThingsboardMqttClient at {} : {} : {}", System.currentTimeMillis(), host, port);
        this.HOST = host;
        this.PORT = port;
        this.TIMEOUT = port;
    }

    @Override
    public void start() {
        // ignore
    }

    @Override
    public void stop() {
        // ignore
    }
    // org.eclipse.paho
    @Override
    public void send(final String token, final ArrayList<String> msg) {
        localSend(token, msg);
    }
    public void localSend(final String deviceToken, final ArrayList<String> msg) {
        LOG.debug("MQTT Publish for {} at {}; Payloads: {}", deviceToken, System.currentTimeMillis(), String.join("; ", msg));
        int qos = 2;
        String broker = this.HOST + ":" + this.PORT;
        MemoryPersistence persistence = new MemoryPersistence();

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setUserName(deviceToken.trim());
        connOpts.setCleanSession(true);
        connOpts.setConnectionTimeout(TIMEOUT);

        try (MqttClient sampleClient = new MqttClient(broker, deviceToken.trim(), persistence)) {
            sampleClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    LOG.debug("Mqtt connectionLost for {} on {}", deviceToken.trim(), cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    LOG.debug("Mqtt messageArrived for {} on {} : {}", deviceToken.trim(), topic, message.toString());

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    LOG.debug("Mqtt deliveryComplete for {} : {}", deviceToken.trim(), token.getMessageId());
                }

            });
            sampleClient.connect(connOpts);
            for (final String s : msg) { 
                MqttMessage message = new MqttMessage(s.getBytes());
                message.setQos(qos);
                sampleClient.publish(mTopic, message);
            }
            sampleClient.disconnect();
            LOG.debug("Mqtt disconnected for {}", deviceToken.trim());
        } catch(MqttException me) {
            LOG.error("Error on MqttClient for device {} : {}", deviceToken.trim(), me.getMessage());
        }
    }
}