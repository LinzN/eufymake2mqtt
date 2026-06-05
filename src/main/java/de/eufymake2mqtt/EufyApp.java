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

import de.eufymake2mqtt.eufy.EufyManager;
import de.eufymake2mqtt.mirror.MirrorManager;

public class EufyApp {

    private final Configuration configuration;
    private final EufyManager eufyManager;
    private final MirrorManager mirrorManager;

    public EufyApp() {
        this.configuration = new Configuration();
        this.eufyManager = new EufyManager(this);
        this.eufyManager.connect();
        this.mirrorManager = new MirrorManager(this);
        this.mirrorManager.connect();
    }

    public static void main(String[] args) {
        new EufyApp();
    }

    public EufyManager getEufyManager() {
        return eufyManager;
    }

    public MirrorManager getMirrorManager() {
        return mirrorManager;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
