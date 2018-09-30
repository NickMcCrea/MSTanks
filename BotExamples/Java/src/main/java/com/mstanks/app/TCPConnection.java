package com.mstanks.app;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.*;

import static java.net.StandardSocketOptions.SO_RCVBUF;

public class TCPConnection implements Runnable  {

    private static Logger log = LogManager.getLogger(TCPConnection.class);
    private volatile boolean connected = false;
    private String host;
    private int port;
    private AsynchronousSocketChannel socket;
    private ConcurrentLinkedQueue<byte[]> incommingMessages;
    private ConcurrentLinkedQueue<byte[]> outgoingMessages;
    private volatile boolean quit = false;

    public TCPConnection(String host, int port, ConcurrentLinkedQueue<byte[]> incomingMessages, ConcurrentLinkedQueue<byte[]> outgoingMessages){
        this.host = host;
        this.port = port;
        this.incommingMessages = incomingMessages;
        this.outgoingMessages = outgoingMessages;
    }

    public void connect(){
        try {
            socket = AsynchronousSocketChannel.open();
            socket.setOption(SO_RCVBUF, 512);
            //try to connect to the server side
            socket.connect( new InetSocketAddress(host, port), socket, new CompletionHandler<>() {
                @Override
                public void completed(Void result, AsynchronousSocketChannel channel ) {
                    log.info("Socket successfully setup.");
                    connected = true;
                    process();
                }

                @Override
                public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                    connected = false;
                    log.fatal("Unable to connect to socket");
                }

            });
        }catch (IOException ioe){
            log.fatal("Socket failed: " + ioe.getMessage());
            //not going to do anything special here..
        }
    }

    private void read(){
        ByteBuffer buffer = ByteBuffer.allocate(512);

        try {
            socket.read(buffer, socket, new CompletionHandler<Integer, AsynchronousSocketChannel>() {

                @Override
                public void completed(Integer result, AsynchronousSocketChannel channel) {
                    //message is read from server
                    if (result < 0) {
                        log.info("end of result.");
                        buffer.clear();
                        // handle unexpected connection close
                    }
                    else if (buffer.hasRemaining()) {
                        // repeat the call with the same CompletionHandler
                        log.info("result: " + result.toString());
                        channel.read(buffer, channel, this);
                    }
                    else {
                        log.info("Cannot read anymore into 512byte buffer, returning message.");
                        incommingMessages.add(buffer.rewind().array());
                        buffer.clear();
                    }

                }
                @Override
                public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                    log.warn("fail to read message from server");
                    //we should do some work with exc to determine exception and if connection lost/recoverable
                    connected = false;
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
                    //log.info("Whooosh, there goes that message!");
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
