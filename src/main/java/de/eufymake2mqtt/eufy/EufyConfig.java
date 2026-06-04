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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EufyConfig {

    private JSONObject config = null;
    private File sslCertificate;

    public EufyConfig(){
        Path configDir = Paths.get("eufy");
        Path configFile = configDir.resolve("default.json");
        Path sslCertificatePath = configDir.resolve("ankermake_ca.pem");
        try {
            Files.createDirectories(configDir);

            if(!configFile.toFile().exists()){
                System.out.println("Error: "+configFile.getFileName()+" does not exist.");
            } else {
                String jsonContent = Files.readString(configFile);
                config = new JSONObject(jsonContent);
            }

            if(!sslCertificatePath.toFile().exists()){
                System.out.println("Error: "+sslCertificatePath.getFileName()+" does not exist.");
            } else {
                this.sslCertificate = sslCertificatePath.toFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean hasConfig(){
        return config != null && sslCertificate != null;
    }

    public JSONObject getAccountData(){
        return config.getJSONObject("account");
    }
    public JSONArray getPrinters(){
        return config.getJSONArray("printers");
    }

    public File getSslCertificate() {
        return sslCertificate;
    }
}
