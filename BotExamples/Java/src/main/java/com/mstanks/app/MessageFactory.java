package com.mstanks.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class MessageFactory  {

    private static Logger log = LogManager.getLogger(MessageFactory.class);
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

    private static byte[] addToFront(NetworkMessageType commandType, byte[] message){
        byte[] msgAdd = new byte[message.length + 1];
        msgAdd[0] = (byte)commandType.code;
        System.arraycopy(message, 0, msgAdd, 1, message.length);
        return msgAdd;
    }

    public static Pair<NetworkMessageType, String> decodeMessage(byte[] message){
        byte[] incomingMessage = new byte[message.length-1];
        System.arraycopy(message, 1, incomingMessage, 0, incomingMessage.length);
        String decodedMsg = byteArrToString(incomingMessage);
        int code = (int)message[0];
        log.info(String.format("Received message of [%d]: %s", code, decodedMsg));
        return Pair.of(NetworkMessageType.get(code), decodedMsg);
    }

    public static byte[] createTankMessage(String tankName, String color)
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
        log.info("JSON String: " + json);
        byte[] clientMessageAsByteArray = stringTobyteArr(json);
        return addToFront(NetworkMessageType.CREATE_TANK, clientMessageAsByteArray);
    }

    public enum NetworkMessageType
    {
        TEST (0),
        CREATE_TANK (1),
        DESPAWN_TANK (2),
        FIRE (3),
        FORWARD (4),
        REVERSE (5),
        LEFT (6),
        RIGHT (7),
        STOP (8),
        TURRET_LEFT (9),
        TURRET_RIGHT (10),
        STOP_TURRET (11),
        OBJECT_UPDATE (12)
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
