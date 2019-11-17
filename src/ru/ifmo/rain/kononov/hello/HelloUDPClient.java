package ru.ifmo.rain.kononov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {
    private static final int SOCKET_TIMEOUT = 500;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Threads number less than 1");
        }

        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.err.println("Cannot find host: " + host);
            return;
        }

        final SocketAddress dst = new InetSocketAddress(addr, port);
        final ExecutorService workers = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int id = i;
            workers.submit(() -> sendAndReceive(dst, prefix, requests, id));
        }

        workers.shutdown();
        try {
            workers.awaitTermination(requests * 5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private static void sendAndReceive(final SocketAddress addr, final String prefix, int cnt, int id) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SOCKET_TIMEOUT);
            final byte[] buffer = new byte[socket.getReceiveBufferSize()];
            final DatagramPacket msg = MsgUtils.newEmptyMessage();
            msg.setSocketAddress(addr);
            for (int ind = 0; ind < cnt; ind++) {
                final String requestText = prefix + id + "_" + ind;
                while (!socket.isClosed() || Thread.currentThread().isInterrupted()) {
                    try {
                        MsgUtils.setText(msg, requestText);
                        socket.send(msg);
                        System.out.println("Request: " + requestText + "\n");
                        msg.setData(buffer);
                        socket.receive(msg);
                        String responseText = MsgUtils.getText(msg);
                        if (responseText.contains(requestText)) {
                            System.out.println("Response: " + responseText + "\n");
                            break;
                        }
                    } catch (IOException e) {
                        System.err.println("Error while processing request: " + e.getMessage() + "\n");
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Cannot create socket: " + addr.toString() + "\n");
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("5 arguments required");
            return;
        }
        for (int i = 0; i < 5; i++) {
            if (args[i] == null) {
                System.err.println("Null argument at position " + i);
                return;
            }
        }
        try {
            new HelloUDPClient().run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } catch (NumberFormatException e) {
            System.err.println("Integer required: " + e.getMessage());
        }
    }
}
