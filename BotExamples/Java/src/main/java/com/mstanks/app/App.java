package com.mstanks.app;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.Configurator;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.LogManager;

public class App implements Runnable
{
    static final Logger log = LoggerFactory.getLogger(App.class);

    private ConcurrentLinkedQueue<byte[]> incomingMessages;
    private TCPConnectionSimple tcpConn;
    private String host;
    private int port;
    private boolean quit = false;
    private GameObjectState ourState;
    private String tankName;

    public App(){
        setup();
    }

    private void setup(){

        host = System.getProperty("host", "localhost");
        port = Integer.getInteger("port", 8052);
        tankName = System.getProperty("name", "JavaBot");
        log.warn(System.getProperties().toString());
        incomingMessages = new ConcurrentLinkedQueue<>();
        tcpConn =  new TCPConnectionSimple(host, port, incomingMessages);
        tcpConn.connect();
        log.debug("App setup.");
    }

    private void process() throws InterruptedException {

        if(!incomingMessages.isEmpty()){
            byte[] message = incomingMessages.poll();
            decodeMessage(message);
            //do something with decoded message/info
            //doSomeLogic();
        }

        //wait until we get our first state update from the server
        if (ourState != null)
        {
            //let's turn the tanks turret towards a random point.
            Random r = new Random();
            int randomTurretX = TankUtils.getNext(-70, 70);
            int randomTurretY = TankUtils.getNext(-100, 100);

            //let's turn the tanks turret towards a random point.
            float targetHeading = TankUtils.getHeading(ourState.x, ourState.y, randomTurretX, randomTurretY);
            sendMessage(MessageFactory.createMovementMessage(MessageFactory.NetworkMessageType.TURN_TURRET_TO_HEADING, targetHeading));

            Thread.sleep(200);


            //now let's turn the whole vehicle towards a different random point.
            int randomX = TankUtils.getNext(-70, 70);
            int randomY = TankUtils.getNext(-100, 100);
            float targetHeading2 = TankUtils.getHeading(ourState.x, ourState.y, randomX, randomY);
            sendMessage(MessageFactory.createMovementMessage(MessageFactory.NetworkMessageType.TURN_TO_HEADING, targetHeading));

            Thread.sleep(200);

            //now let's move to that point.
            float distance = TankUtils.calculateDistance(ourState.x, ourState.y, randomX, randomY);
            sendMessage(MessageFactory.createMovementMessage(MessageFactory.NetworkMessageType.MOVE_FORWARD_DISTANCE, distance));

            Thread.sleep(200);
        }

    }

    private void createTestTank(){
        byte[] message = MessageFactory.createTankMessage(tankName);
        tcpConn.write(message);
    }


    private void aimTurretToTargetHeading(float targetHeading)
    {
        float turretDiff = targetHeading - ourState.turretHeading;
        if (Math.abs(turretDiff) < 5)
        {
            sendMessage(MessageFactory.createMessage(MessageFactory.NetworkMessageType.STOP_TURRET));

        }
        else if (TankUtils.isTurnLeft(ourState.turretHeading, targetHeading))
        {
            sendMessage(MessageFactory.createMessage(MessageFactory.NetworkMessageType.TOGGLE_TURRET_LEFT));

        }
        else if (!TankUtils.isTurnLeft(ourState.turretHeading, targetHeading))
        {
            sendMessage(MessageFactory.createMessage(MessageFactory.NetworkMessageType.TOGGLE_TURRET_RIGHT));
        }
    }

    private void decodeMessage(byte[] message){
        Pair<MessageFactory.NetworkMessageType, String> incMessage;
        incMessage = MessageFactory.decodeMessage(message);
        MessageFactory.NetworkMessageType mType = incMessage.getLeft();
        String jsonString = incMessage.getRight();

        switch(mType){
            case OBJECT_UPDATE: {
                decodeGameState(jsonString);
            }
            default:
                log.debug(String.format("Received message of type %s", mType));
            //case OTHER CASES

        }
    }

    private void sendMessage(byte[] message){
        log.debug("sending message");
        tcpConn.write(message);
    }

    private GameObjectState decodeGameState(String state){
        log.debug("Decode game state: " + state);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        //required as JSON in C# style casing
        try {
            GameObjectState gState = objectMapper.readValue(state, GameObjectState.class);
            log.debug(gState.toString());
            if(gState.name.equals(tankName)){
                ourState = gState;
            }
            return gState;
        } catch (IOException e) {
            log.warn("Unable to parse game state: " + e.getMessage());
            return null;
        }
    }



    private void start(){
        Thread t = new Thread(this);
        t.setName("Main Thread Loop");
        t.start();
    }



    @Override
    public void run() {
        tcpConn.start();
        try {
            while(!tcpConn.isConnected()){
                Thread.sleep(100);
                log.debug("Waiting for connection setup");
            }

            createTestTank();
            while (tcpConn.isConnected() && !quit) {

                Thread.sleep(50);
                process();
            }
        }catch(InterruptedException ie){
            log.warn("IOException in main loop: " + ie.getMessage());
            log.warn("die.");
        }
    }


    public static void main( String[] args )
    {
        App a = new App();
        a.start();
    }

}
