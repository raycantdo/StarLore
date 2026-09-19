package com.starlore.starlore;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Client-side networking manager for Constellation Duel.
 * Connects to DuelServer, listens in a daemon background thread,
 * and safely delivers all game events to the JavaFX thread via Platform.runLater.
 */
public class DuelSocketClient {

    private Socket socket;
    private PrintWriter out;
    private volatile boolean running = false;

    public interface DuelListener {
        void onOnlineUsers(List<String> onlineUsernames);
        void onDuelInvite(String fromUser, String constellation);
        void onDuelAccepted(String fromUser, String constellation);
        void onDuelDeclined(String fromUser);
        void onRemoteLink(String fromUser, int starA, int starB);
        void onRematch(String fromUser);
        void onOpponentLeft(String fromUser);
        void onDisconnected();
    }

    /**
     * Connects to DuelServer, sends the initial HELLO handshake, and begins background listening.
     */
    public void connect(String host, int port, String username, DuelListener listener) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true); // autoFlush = true
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Handshake identifying this stargazer
        out.println("HELLO:" + username);
        running = true;

        Thread listenerThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    int firstColon = line.indexOf(':');
                    if (firstColon < 0) continue;

                    String tag = line.substring(0, firstColon);
                    String rest = line.substring(firstColon + 1);

                    switch (tag) {
                        case "ONLINE": {
                            List<String> online = rest.trim().isEmpty()
                                    ? Collections.emptyList()
                                    : new ArrayList<>(Arrays.asList(rest.split(",")));
                            Platform.runLater(() -> listener.onOnlineUsers(online));
                            break;
                        }
                        case "INVITE": {
                            int secondColon = rest.indexOf(':');
                            if (secondColon < 0) continue;
                            String fromUser = rest.substring(0, secondColon);
                            String constellation = rest.substring(secondColon + 1);
                            Platform.runLater(() -> listener.onDuelInvite(fromUser, constellation));
                            break;
                        }
                        case "ACCEPT": {
                            int secondColon = rest.indexOf(':');
                            if (secondColon < 0) continue;
                            String fromUser = rest.substring(0, secondColon);
                            String constellation = rest.substring(secondColon + 1);
                            Platform.runLater(() -> listener.onDuelAccepted(fromUser, constellation));
                            break;
                        }
                        case "DECLINE": {
                            String fromUser = rest.trim();
                            Platform.runLater(() -> listener.onDuelDeclined(fromUser));
                            break;
                        }
                        case "LINK": {
                            int secondColon = rest.indexOf(':');
                            if (secondColon < 0) continue;
                            String fromUser = rest.substring(0, secondColon);
                            String coords = rest.substring(secondColon + 1);
                            String[] parts = coords.split(",");
                            if (parts.length == 2) {
                                int starA = Integer.parseInt(parts[0]);
                                int starB = Integer.parseInt(parts[1]);
                                Platform.runLater(() -> listener.onRemoteLink(fromUser, starA, starB));
                            }
                            break;
                        }
                        case "REMATCH": {
                            String fromUser = rest.trim();
                            Platform.runLater(() -> listener.onRematch(fromUser));
                            break;
                        }
                        case "LEAVE": {
                            String fromUser = rest.trim();
                            Platform.runLater(() -> listener.onOpponentLeft(fromUser));
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                // Socket closed or connection lost
            } finally {
                if (running) {
                    running = false;
                    Platform.runLater(listener::onDisconnected);
                }
            }
        });

        listenerThread.setDaemon(true); // Don't hold the JVM open on app exit
        listenerThread.start();
    }

    public void sendInvite(String toUser, String constellation) {
        if (out != null) out.println("INVITE:" + toUser + ":" + constellation);
    }

    public void acceptInvite(String toUser, String constellation) {
        if (out != null) out.println("ACCEPT:" + toUser + ":" + constellation);
    }

    public void declineInvite(String toUser) {
        if (out != null) out.println("DECLINE:" + toUser);
    }

    public void sendLink(String toUser, int starA, int starB) {
        if (out != null) out.println("LINK:" + toUser + ":" + starA + "," + starB);
    }

    public void sendRematch(String toUser) {
        if (out != null) out.println("REMATCH:" + toUser);
    }

    public void sendLeave(String toUser) {
        if (out != null) out.println("LEAVE:" + toUser);
    }

    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void close() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}
