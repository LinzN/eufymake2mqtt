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


import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EufyDecoder {

    private static final byte[] IV =
            "3DPrintAnkerMake".getBytes(StandardCharsets.UTF_8);

    public static DecodedMessage decode(byte[] mqttPacket, byte[] aesKey)
            throws Exception {

        if (xorBytes(mqttPacket) != 0) {
            System.out.println("WARNUNG: MQTT Checksumme ungültig");
        }

        byte[] packet = Arrays.copyOf(mqttPacket, mqttPacket.length - 1);

        int format = packet[6] & 0xFF;

        int bodyLength;
        switch (format) {
            case 1:
                bodyLength = 24;   // M5C
                break;
            case 2:
                bodyLength = 64;   // M5
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported MQTT format: " + format);
        }

        byte[] header = Arrays.copyOfRange(packet, 0, bodyLength);
        byte[] encryptedPayload =
                Arrays.copyOfRange(packet, bodyLength, packet.length);

        byte[] decryptedPayload =
                aesCbcDecrypt(encryptedPayload, aesKey);

        byte[] completeMessage =
                concat(header, decryptedPayload);

        return parseMessage(completeMessage);
    }

    private static DecodedMessage parseMessage(byte[] data)
            throws Exception {

        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        byte[] signature = new byte[2];
        bb.get(signature);

        String sig = new String(signature, StandardCharsets.US_ASCII);
        if (!"MA".equals(sig)) {
            throw new IllegalArgumentException(
                    "Ungültige Signatur: " + sig);
        }

        DecodedMessage msg = new DecodedMessage();

        msg.size = bb.getShort() & 0xFFFF;
        msg.m3 = bb.get() & 0xFF;
        msg.m4 = bb.get() & 0xFF;
        msg.m5 = bb.get() & 0xFF;
        msg.m6 = bb.get() & 0xFF;
        msg.m7 = bb.get() & 0xFF;

        msg.packetType = bb.get() & 0xFF;
        msg.packetNum = bb.getShort() & 0xFFFF;

        if (msg.m5 == 2) {

            msg.timestamp = Integer.toUnsignedLong(bb.getInt());

            byte[] guidBytes = new byte[37];
            bb.get(guidBytes);

            msg.deviceGuid =
                    new String(guidBytes, StandardCharsets.UTF_8)
                            .replace("\0", "");

            bb.position(bb.position() + 11);

        } else if (msg.m5 == 1) {

            msg.timestamp = 0;
            msg.deviceGuid = "";

            bb.position(bb.position() + 12);
        }

        msg.payload = new byte[bb.remaining()];
        bb.get(msg.payload);

        try {
            ObjectMapper mapper = new ObjectMapper();
            msg.json = mapper.readTree(msg.payload);
        } catch (Exception ignored) {
        }

        return msg;
    }

    private static byte[] aesCbcDecrypt(byte[] encrypted, byte[] key)
            throws Exception {

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        SecretKeySpec keySpec =
                new SecretKeySpec(key, "AES");

        IvParameterSpec ivSpec =
                new IvParameterSpec(IV);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        return cipher.doFinal(encrypted);
    }

    private static int xorBytes(byte[] data) {
        int result = 0;

        for (byte b : data) {
            result ^= (b & 0xFF);
        }

        return result;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];

        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);

        return result;
    }

    public static class DecodedMessage {
        public int size;
        public int m3;
        public int m4;
        public int m5;
        public int m6;
        public int m7;
        public int packetType;
        public int packetNum;
        public long timestamp;
        public String deviceGuid;
        public byte[] payload;
        public JsonNode json;
    }
}