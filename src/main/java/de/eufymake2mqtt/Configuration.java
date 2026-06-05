
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

package de.eufymake2mqtt;

import de.linzn.simplyConfiguration.FileConfiguration;
import de.linzn.simplyConfiguration.provider.YamlConfiguration;

import java.io.File;

public class Configuration {
    private final FileConfiguration fileConfiguration;
    public String hostname;
    public int port;
    public String username;
    public String password;
    public String mirrorTopic;

    public Configuration() {
        fileConfiguration = YamlConfiguration.loadConfiguration(new File("config.yml"));
        this.hostname = fileConfiguration.getString("mqtt.ip", "127.0.0.1");
        this.port = fileConfiguration.getInt("mqtt.port", 1883);
        this.username = fileConfiguration.getString("mqtt.username", "your_mom");
        this.password = fileConfiguration.getString("mqtt.password", "is_so_fat");
        this.mirrorTopic = fileConfiguration.getString("mqtt.publishTopic", "eufymake2mqtt/printers/{serial}/data");
        fileConfiguration.save();
    }

    public FileConfiguration getFileConfiguration() {
        return this.fileConfiguration;
    }
}
