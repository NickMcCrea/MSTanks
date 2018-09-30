package com.mstanks.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.*;

import javax.sound.sampled.AudioFormat;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class App implements Runnable
{
    private static Logger log = LogManager.getLogger(App.class);

    private ConcurrentLinkedQueue<byte[]> incomingMessages;
    private ConcurrentLinkedQueue<byte[]> outgoingMessages;
    private TCPConnection tcpConn;
    private String host;
    private int port;
    private boolean quit = false;

    public App(){
        setup();
    }

    private void setup(){
        Properties props = new Properties();
        InputStream in = App.class.getResourceAsStream("/config.properties");
        if (Objects.isNull(in)){
            log.warn("No config.properties found on class path; using defaults");
            host = "localhost";
            port = 8052;
        }else {

            log.info("Loading properties file.");
            try (in) {
                props.load(in);

                host = props.getProperty("host", "localhost");
                port = Integer.getInteger(props.getProperty("port", "8052"));
            } catch (IOException ioe) {
                log.warn("Unable to set config proprerties, defaulting.");
                host = "localhost";
                port = 8052;
            }

        }
        incomingMessages = new ConcurrentLinkedQueue<>();
        outgoingMessages = new ConcurrentLinkedQueue<>();

        tcpConn =  new TCPConnection(host, port, incomingMessages, outgoingMessages);
        tcpConn.connect();
        log.info("App setup.");
    }

    private void process(){

        if(!incomingMessages.isEmpty()){
            byte[] message = incomingMessages.poll();
            decodeMessage(message);
            //do something with decoded message/info
            //doSomeLogic();
        }
        testAction();

    }
    private void createTestTank(){
        byte[] message = MessageFactory.createTankMessage("test", "");
        outgoingMessages.add(message);
    }
    private void testAction(){
        Random r = new Random();
        int nAction = r.nextInt((12-3)+1)+3;
        sendMessage(MessageFactory.NetworkMessageType.get(nAction), "");

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
            //case OTHER CASES

        }
    }

    private void sendMessage(MessageFactory.NetworkMessageType action, String msg){
        byte[] message = MessageFactory.createMessage(action);
        outgoingMessages.add(message);
    }

    private GameObjectState decodeGameState(String state){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true); //required as JSON in C# style casing
        try {
            GameObjectState gState = objectMapper.readValue(state, GameObjectState.class);
            log.info(gState);
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
        try {
            while(!tcpConn.isConnected()){
                Thread.sleep(100);
                log.info("Waiting for connection setup");
            }

            createTestTank();
            while (tcpConn.isConnected() && !quit) {

                Thread.sleep(100);
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
