package com.starlore.starlore;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Minimal TCP networking helper for 2-player Duel Mode.
 * One player hosts (ServerSocket) and waits; the other joins using the host's IP.
 * Messages are plain newline-terminated strings, e.g. "CONST:ARIES", "LINK:2,5".
 */
public class NetworkManager {

    public static final int DEFAULT_PORT = 5050;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = false;

    /** Blocks until a client connects. Call from a background thread, never the FX thread. */
    public void hostAndWaitForClient(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            socket = serverSocket.accept(); // blocks here until the friend joins
        }
        setupStreams();
    }

    /** Blocks until the connection succeeds. Call from a background thread, never the FX thread. */
    public void connectToHost(String ipAddress, int port) throws IOException {
        socket = new Socket(ipAddress, port);
        setupStreams();
    }

    private void setupStreams() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true); // autoFlush = true
    }

    /** Starts a background reader thread; each incoming line is delivered on the FX thread via onMessage. */
    public void startListening(Consumer<String> onMessage, Runnable onDisconnect) {
        running = true;
        Thread listenerThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    String received = line;
                    Platform.runLater(() -> onMessage.accept(received));
                }
            } catch (IOException e) {
                // connection dropped — handled in the finally block below
            } finally {
                if (running) {
                    running = false;
                    Platform.runLater(onDisconnect);
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /** Sends one line to the other player. Safe to call from the FX thread. */
    public void send(String message) {
        if (out != null) out.println(message);
    }

    public void close() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}