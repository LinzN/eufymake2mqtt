/*
 * Copyright (c) 2026 MirraNET, Niklas Linz. All rights reserved.
 *
 * This file is part of the MirraNET project and is licensed under the
 * GNU Lesser General Public License v3.0 (LGPLv3).
 *
 * You may use, distribute and modify this code under the terms
 * of the LGPLv3 license. You should have received a copy of the
 * license along with this file. If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>
 * or contact: niklas.linz@mirranet.de
 */

package de.eufymake2mqtt.mirror;

import de.eufymake2mqtt.EufyApp;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;

import java.util.concurrent.Executors;

public class MirrorManager implements MqttCallbackExtended {

    private final EufyApp eufyApp;
    private final MqttClient mqttClient;
    private final MqttConnectOptions opts;
    private final String publishTopic;

    public MirrorManager(EufyApp eufyApp) {
        this.eufyApp = eufyApp;
        String brokerUrl = "tcp://" + this.eufyApp.getConfiguration().hostname + ":" + this.eufyApp.getConfiguration().port;
        this.publishTopic = this.eufyApp.getConfiguration().mirrorTopic;
        try {
            mqttClient = new MqttClient(brokerUrl, "eufymake2mqtt", new MemoryPersistence());
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
        mqttClient.setCallback(this);
        opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setUserName(this.eufyApp.getConfiguration().username);
        opts.setPassword(this.eufyApp.getConfiguration().password.toCharArray());
        opts.setConnectionTimeout(15);
        opts.setKeepAliveInterval(30);
        opts.setMaxReconnectDelay(30000);
        opts.setAutomaticReconnect(true);
    }

    public void connect() {
        try {
            mqttClient.connect(opts);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
        if (mqttClient.isConnected()) {
            System.out.println("Connected to local mqtt broker for mirroring!");
        }
    }

    public void mirror(String printerSerial, JSONArray jsonArray) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(jsonArray.toString().getBytes());
        mqttMessage.setQos(1);
        if (this.mqttClient.isConnected()) {
            try {
                System.out.println("Mirror data to " + this.publishTopic.replace("{serial}", printerSerial));
                this.mqttClient.publish(this.publishTopic.replace("{serial}", printerSerial), mqttMessage);
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connection lost to local mqtt broker for mirroring!" + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            System.out.println("Reconnected to local mqtt broker for mirroring!");
        }
    }
}
