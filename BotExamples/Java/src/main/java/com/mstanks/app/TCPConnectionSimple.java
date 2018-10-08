package com.mstanks.app;

import com.google.common.primitives.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TCPConnectionSimple implements Runnable {

    static final Logger log = LoggerFactory.getLogger(TCPConnectionSimple.class);
    private volatile boolean connected = false;
    private String host;
    private int port;
    private Socket socket;
    private ConcurrentLinkedQueue<byte[]> incommingMessages;
    private volatile boolean quit = false;
    private InputStream inStream;
    private OutputStream outStream;

    public TCPConnectionSimple(String host, int port, ConcurrentLinkedQueue<byte[]> incomingMessages){
        this.host = host;
        this.port = port;
        this.incommingMessages = incomingMessages;
    }

    public void connect(){
        try {
            //try to connect to the server side
                socket = new Socket(host, port);

                outStream = socket.getOutputStream();
                inStream= socket.getInputStream();
                connected = true;
                log.debug("Socket connected and streams setup.");
            } catch (UnknownHostException e) {
                    log.warn("Unable to connect to host.");
                    e.printStackTrace();
            } catch (IOException e) {
                    log.warn("Exception encountered during connect.");
                    e.printStackTrace();
            }
    }

    private void read(){
        while(connected) {

            byte[] header = new byte[2];
            byte[] packet;
            try {
                log.debug("Read 2 header bytes.");
                inStream.read(header, 0, 2);

                int payloadLength = Byte.toUnsignedInt(header[1]);
                log.debug(String.format("Packet detected, payload length of %d.", payloadLength));
                byte[] payload = new byte[0];
                if (payloadLength > 0) {
                    payload = new byte[payloadLength];
                    inStream.read(payload, 0, payloadLength);
                }
                packet = Bytes.concat(header, payload);
                log.debug(String.format("Recieved total packet of %d bytes", packet.length));
                incommingMessages.add(packet);

            } catch (IOException e) {
                e.printStackTrace();
                connected = false;
            }
        }
    }


    public void write(byte[] message) {
        try {
            log.debug(String.format("Sending packet of %d bytes.", message.length));
            outStream.write(message);
            log.debug("Message written.");
        } catch (IOException e) {
            log.warn("Exception encountered during write");
            e.printStackTrace();
            connected = false;
        }
    }

    public void setConnected(boolean connected){
        this.connected = quit;

    }

    public boolean isConnected(){
        return connected;
    }

    public void start(){
        Thread t = new Thread(this);
        t.setName("TCP Connection Thread");
        t.start();
    }

    @Override
    public void run() {
        read();
    }
}
