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

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class EufyManager {
    private final EufyConfig eufyConfig;
    private final Map<String, EufyMakePrinter> printers;

    private EufyCredentials eufyCredentials;
    private File sslCertificate;

    public EufyManager(){
        this.printers = new HashMap<>();
        this.eufyConfig = new EufyConfig();
        this.load();
    }

    private void load(){
        if(this.eufyConfig.hasConfig()){
            String userId = this.eufyConfig.getAccountData().getString("user_id");
            String email = this.eufyConfig.getAccountData().getString("email");
            String region = this.eufyConfig.getAccountData().getString("region");
            this.eufyCredentials = new EufyCredentials("eufy_" + userId, email);

            JSONArray jsonPrinters = this.eufyConfig.getPrinters();
            for(int i = 0; i < jsonPrinters.length(); i++){
                JSONObject printer = jsonPrinters.getJSONObject(i);
                String serialNumber = printer.getString("sn");
                String mqttKey = printer.getString("mqtt_key");
                try {
                    EufyMakePrinter eufyMakePrinter = new EufyMakePrinter(serialNumber, region, mqttKey, eufyCredentials, this);
                    this.printers.put(serialNumber, eufyMakePrinter);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            System.err.println("No config found!");
        }
    }

    public void connect(){
        if(this.eufyConfig.hasConfig()) {
            for (EufyMakePrinter eufyMakePrinter : this.printers.values()) {
                try {
                    if (eufyMakePrinter.connect()) {
                        System.out.println("EufyMakePrinter " + eufyMakePrinter.getSerialNumber() + " connected to mqtt");
                    }
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else {
            System.err.println("No config found!");
        }
    }

    public void disconnect(){
        if(this.eufyConfig.hasConfig()) {
            for (EufyMakePrinter eufyMakePrinter : this.printers.values()) {
                try {
                    eufyMakePrinter.disconnect();
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            System.err.println("No config found!");
        }
    }

    public File getSslCertificate() {
        return sslCertificate;
    }
}
