package ru.ifmo.rain.kononov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService workers;
    private ExecutorService receiver;
    private boolean closed = true;
    private int inBuffSize = 0;

    private static final int TERMINATION_TIMEOUT = 10;

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            inBuffSize = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            System.err.println("Cannot create socket on port: " + port);
            return;
        }
        workers = Executors.newFixedThreadPool(threads);
        receiver = Executors.newSingleThreadExecutor();
        closed = false;
        receiver.submit(this::receiveAndRespond);
    }

    private void receiveAndRespond() {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                final DatagramPacket msg = MsgUtils.newMessage(inBuffSize);
                socket.receive(msg);
                workers.submit(() -> sendResponse(msg));
            } catch (IOException e) {
                if (!closed) {
                    System.err.println("Error occurred during processing datagram: " + e.getMessage() + "\n");
                }
            }
        }
    }

    private void sendResponse(final DatagramPacket msg) {
        final String msgText = MsgUtils.getText(msg);
        try {
            MsgUtils.setText(msg, "Hello, " + msgText);
            socket.send(msg);
        } catch (IOException e) {
            if (!closed) {
                System.err.println("Error occurred during processing datagram: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        socket.close();
        workers.shutdown();
        receiver.shutdown();
        try {
            workers.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS);
            receiver.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("2 arguments required");
            return;
        }
        for (int i = 0; i < 2; i++)
            if (args[i] == null) {
                System.err.println("Null argument at position: " + i);
                return;
            }
        try (HelloUDPServer server = new HelloUDPServer()) {
            server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            System.err.println("Integer expected: " + e.getMessage());
        }
    }
}
