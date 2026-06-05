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

package de.eufymake2mqtt.eufy;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.HexFormat;
import java.util.concurrent.Executors;

public class EufyMakePrinter implements MqttCallbackExtended {

    private static final String BROKER_HOST_EU = "make-mqtt-eu.ankermake.com";
    private static final String BROKER_HOST_US = "make-mqtt.ankermake.com";
    private static final int BROKER_PORT = 8789;  // MQTT TLS

    private final String serialNumber;
    private final String region;
    private final byte[] aesKey;
    private final MqttClient mqttClient;
    private final EufyCredentials eufyCredentials;
    private final MqttConnectOptions opts;
    private final EufyManager eufyManager;

    public EufyMakePrinter(String serialNumber, String region, String mqttKey, EufyCredentials eufyCredentials, EufyManager eufyManager) throws Exception {
        this.serialNumber = serialNumber;
        this.region = region;
        this.aesKey = HexFormat.of().parseHex(mqttKey);
        this.eufyCredentials = eufyCredentials;
        this.eufyManager = eufyManager;

        String brokerUrl = "ssl://" + (region.equalsIgnoreCase("eu") ? BROKER_HOST_EU : BROKER_HOST_US) + ":" + BROKER_PORT;
        mqttClient = new MqttClient(brokerUrl, eufyCredentials.userId() + "_" + serialNumber, new MemoryPersistence(), Executors.newScheduledThreadPool(2));
        mqttClient.setCallback(this);

        opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setUserName(eufyCredentials.userId());
        opts.setPassword(eufyCredentials.password().toCharArray());
        opts.setConnectionTimeout(15);
        opts.setKeepAliveInterval(30);
        opts.setMaxReconnectDelay(30000);
        opts.setSocketFactory(buildSslContext().getSocketFactory()); // oder echtes Cert
        opts.setAutomaticReconnect(true);
    }


    public String getSerialNumber() {
        return serialNumber;
    }

    public String getRegion() {
        return region;
    }

    public EufyCredentials getEufyCredentials() {
        return eufyCredentials;
    }

    public boolean connect() throws MqttException {
        mqttClient.connect(opts);
        //mqttClient.subscribe("/phone/maker/" + this.serialNumber + "/notice", 1);
        //mqttClient.subscribe("/phone/maker/" + this.serialNumber + "/command/reply");
        //mqttClient.subscribe("/phone/maker/" + this.serialNumber + "/query/reply");
        return mqttClient.isConnected();
    }

    public void disconnect() throws MqttException {
        mqttClient.disconnect();
    }


    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Lost connection to EufyMake cloud:" + this.serialNumber + " " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        byte[] payload = message.getPayload();
        try {
            EufyDecoder.DecodedMessage ff = EufyDecoder.decode(payload, this.aesKey);
            JSONArray arr;
            try {
                arr = new JSONArray(ff.json.toString());
            } catch (JSONException e) {
                arr = new JSONArray().put(new JSONObject(ff.json.toString()));
            }
            System.out.println("Topic: " + topic + " Decoded JSON: " + arr.toString());
            this.eufyManager.getEufyApp().getMirrorManager().mirror(this.serialNumber, arr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            System.out.println("Reconnected to EufyMake cloud:" + this.serialNumber);
        }
        try {
            System.out.println("Subscribing to topics for " + this.serialNumber);
            mqttClient.subscribe("/phone/maker/" + this.serialNumber + "/notice", 1);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private SSLContext buildSslContext() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate caCert;
        try (InputStream is = new FileInputStream(this.eufyManager.getEufyConfig().getSslCertificate())) {
            caCert = cf.generateCertificate(is);
        }

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("ankermake-ca", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx;
    }

}
