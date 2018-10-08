package com.mstanks.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class MessageFactory  {

    static final Logger log = LoggerFactory.getLogger(MessageFactory.class);
    private static Charset characterSet = Charset.forName("US-ASCII");

    private static byte[] stringTobyteArr(String msg){
        return msg.getBytes(characterSet);
    }

    private static String byteArrToString(byte[] arr){
        return new String(arr, characterSet);
    }

    public static byte[] createMessage(NetworkMessageType commandType){
        String message = "";
        byte[] msg = stringTobyteArr(message);
        return addToFront(commandType,msg);
    }

    public static byte[] createMessage(NetworkMessageType commandType, String message){
        byte[] msg = stringTobyteArr(message);
        return addToFront(commandType,msg);
    }

    private static byte[] addToFront(NetworkMessageType commandType, byte[] payload){
        byte[] msg = new byte[payload.length + 2];
        msg[0] = (byte)commandType.code;
        msg[1] = (byte)payload.length;
        System.arraycopy(payload, 0, msg, 2, payload.length);
        log.debug(String.format("Message created of command type: %s", commandType));
        return msg;
    }

    public static Pair<NetworkMessageType, String> decodeMessage(byte[] message){
        int code = (int)message[0];
        int payloadLength = Byte.toUnsignedInt(message[1]);
        byte[] payload = new byte[payloadLength];
        System.arraycopy(message, 2, payload, 0, payloadLength);
        String decodedMsg = byteArrToString(payload);
        log.debug(String.format("Received message of [%d]: %s", code, decodedMsg));
        return Pair.of(NetworkMessageType.get(code), decodedMsg);
    }

    public static byte[] createTankMessage(String tankName)
    {
        ObjectMapper map = new ObjectMapper();
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("Name", tankName);
        String json = "";
        try {
            json = map.writeValueAsString(jsonMap);
        } catch (JsonProcessingException e) {
            log.warn("Unable to convert jsonMap to JSON string.");
        }
        log.debug("JSON String: " + json);

        byte[] clientMessageAsByteArray = stringTobyteArr(json);
        return addToFront(NetworkMessageType.CREATE_TANK, clientMessageAsByteArray);
    }

    public static byte[] createMovementMessage(NetworkMessageType type, float amount)
    {

        ObjectMapper map = new ObjectMapper();
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("Amount", String.valueOf(amount));
        String json = "";
        try {
            json = map.writeValueAsString(jsonMap);
        } catch (JsonProcessingException e) {
            log.warn("Unable to convert jsonMap to JSON string.");
        }
        log.debug("JSON String: " + json);
        byte[] clientMessageAsByteArray = stringTobyteArr(json);
        return addToFront(type, clientMessageAsByteArray);
    }

    public enum NetworkMessageType
    {
        TEST (0),
        CREATE_TANK (1),
        DESPAWN_TANK (2),
        FIRE (3),
        TOGGLE_FORWARD (4),
        TOGGLE_REVERSE (5),
        TOGGLE_LEFT (6),
        TOGGLE_RIGHT (7),
        TOGGLE_TURRET_LEFT (8),
        TOGGLE_TURRET_RIGHT (9),
        TURN_TURRET_TO_HEADING (10),
        TURN_TO_HEADING (11),
        MOVE_FORWARD_DISTANCE ( 12),
        MOVE_BACKWARD_DISTANCE (13),
        STOP_ALL (14),
        STOP_TURN (15),
        STOP_MOVE (16),
        STOP_TURRET (17),
        OBJECT_UPDATE (18),
        HEALTH_PICKUP (19),
        AMMO_PICKUP (20),
        SNITCH_PICKUP (21),
        DESTROYED (22),
        ENTERED_GOAL (23),
        KILL (24),
        SNITCH_APPEARED (25),
        GAME_TIME_UPDATE (26),
        HIT_DETECTED (27),
        SUCCESSFUL_HIT (28)
        ;

        private final int code;

        NetworkMessageType(int code) {
            this.code = code;
        }

        @Override
        public String toString(){
            return String.valueOf(code);
        }

        public static NetworkMessageType get(int code){
            for(NetworkMessageType msg : NetworkMessageType.values()){
                if(code == msg.code) return msg;
            }
            return null;
        }
    }
}
