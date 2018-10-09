package com.mstanks.app;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.primitives.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPConnection implements Runnable  {

    static final Logger log = LoggerFactory.getLogger(TCPConnection.class);
    private volatile boolean connected = false;
    private String host;
    private int port;
    private AsynchronousSocketChannel socket;
    private ConcurrentLinkedQueue<byte[]> incommingMessages;
    private ConcurrentLinkedQueue<byte[]> outgoingMessages;
    private volatile boolean quit = false;
    private volatile boolean reading = false;

    public TCPConnection(String host, int port, ConcurrentLinkedQueue<byte[]> incomingMessages, ConcurrentLinkedQueue<byte[]> outgoingMessages){
        this.host = host;
        this.port = port;
        this.incommingMessages = incomingMessages;
        this.outgoingMessages = outgoingMessages;
    }

    public void connect(){
        try {
            socket = AsynchronousSocketChannel.open();
            //try to connect to the server side
            socket.connect( new InetSocketAddress(host, port), socket, new CompletionHandler<>() {
                @Override
                public void completed(Void result, AsynchronousSocketChannel channel ) {
                    log.debug("Socket successfully setup.");
                    connected = true;
                    process();
                }

                @Override
                public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                    connected = false;
                    log.error("Unable to connect to socket");
                }

            });
        }catch (IOException ioe){
            log.error("Socket failed: " + ioe.getMessage());
            //not going to do anything special here..
        }
    }



    private void read(){
        ByteBuffer buff = ByteBuffer.allocate(400); //bigger than we should ever need
        buff.limit(2); //limit 2 for header

        if(reading) return;

        try {
            reading = true;
            socket.read(buff, socket, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
                byte[] header;
                int payloadLength;
                byte[] payload;
                byte[] completePacket;

                @Override
                public void completed(Integer result, AsynchronousSocketChannel channel) {
                    //message is read from server
                    if (result < 0) {
                        log.debug("end of header result.");
                        buff.clear();
                        // handle unexpected connection close
                    }
                    else if (buff.hasRemaining()) {
                        // repeat the call with the same CompletionHandler
                        try{
                            channel.read(buff, channel, this);
                        }catch(ReadPendingException rpe){
                            //log.warn("Still awaiting completion of previous read.");
                        }
                    }
                    else {
                        if(buff.limit() == 2){ //header check
                            log.debug("Received " + (2-buff.remaining())+ " bytes as header, will determine size of payload.");
                            byte[] headBuff = buff.flip().array();
                            payloadLength = Byte.toUnsignedInt(headBuff[1]);
                            header = Arrays.copyOf(headBuff,2);
                            log.debug(Arrays.toString(header));
                            log.debug("Payload to be " + payloadLength + "bytes.");
                            buff.clear(); //clear what we've read so far.
                            buff.limit(payloadLength); //set limit to payload
                            try{
                                channel.read(buff, channel, this);
                            }catch(ReadPendingException rpe){
                                //log.warn("Still awaiting completion of previous read.");
                            }
                        }else{ //payload check
                            log.debug("Payload received. (" + buff.position() + ")") ;
                            byte[] payloadArr = buff.flip().array();
                            payload = Arrays.copyOf(payloadArr,payloadLength);
                            completePacket = Bytes.concat(header, payload);
                            incommingMessages.add(completePacket);
                            payloadLength = 0;
                            buff.clear();
                            reading = false;
                        }
                    }

                }
                @Override
                public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                    log.warn("fail to read message from server");
                    //we should do some work with exc to determine exception and if connection lost/recoverable
                    connected = false;
                    reading = false;
                }
            });

        }catch(ReadPendingException rpe){
            //log.warn("Still awaiting completion of previous read.");
        }
    }



    public void write() {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        byte[] message;

        message = outgoingMessages.peek(); //peek as we don't know this will be sent successfully
        if(Objects.isNull(message)){
            return;
        }
        buffer.put(message);
        buffer.flip();
        try {
            socket.write(buffer, socket, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
                @Override
                public void completed(Integer result, AsynchronousSocketChannel channel) {
                    //log.debug("Whooosh, there goes that message!");
                    outgoingMessages.remove(); //remove the message now
                }

                @Override
                public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                    log.warn("Fail to write the message to server");
                }
            });

        }catch(WritePendingException wpe){
            //pending write
        }
    }

    public void setQuit(boolean quit){
        this.quit = quit;
    }

    public boolean isConnected(){
        return connected;
    }

    private void process(){
        Thread t = new Thread(this);
        t.setName("TCP Connection Thread");
        t.start();
    }

    @Override
    public void run() {

        while(connected && !quit){
            try {
                Thread.sleep(15);
                read();
                Thread.sleep(15);
                write();
            } catch (InterruptedException e) {
                log.warn("Thread interrupted: " + e.toString());
            }
        }
    }

}
