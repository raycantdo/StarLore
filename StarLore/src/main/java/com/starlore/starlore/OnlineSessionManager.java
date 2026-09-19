package com.starlore.starlore;

import javafx.application.Platform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global singleton managing online player sessions and socket communication with DuelServer.
 * Ensures that as soon as a player enters their username, they are registered as online.
 */
public class OnlineSessionManager {

    private static final OnlineSessionManager INSTANCE = new OnlineSessionManager();

    public static OnlineSessionManager getInstance() {
        return INSTANCE;
    }

    private DuelSocketClient socketClient;
    private String currentUsername = "";
    private final List<String> onlineUsers = new CopyOnWriteArrayList<>();
    private DuelSocketClient.DuelListener activeListener;
    private volatile boolean connecting = false;

    private OnlineSessionManager() {
        socketClient = new DuelSocketClient();
    }

    /**
     * Called when a player enters their username in HelloController.
     * Registers the player with the server and marks them online across the realm.
     */
    public synchronized void login(String username) {
        if (username == null || username.trim().isEmpty()) return;
        this.currentUsername = username.trim();

        if (socketClient != null && socketClient.isConnected()) {
            return;
        }

        connectInBackground();
    }

    public synchronized void connectInBackground() {
        if (connecting || currentUsername.isEmpty()) return;
        connecting = true;

        Thread connectThread = new Thread(() -> {
            try {
                String host = System.getProperty("starlore.server.host", "127.0.0.1");
                int port = Integer.getInteger("starlore.server.port", DuelServer.DEFAULT_PORT);

                if (socketClient != null && socketClient.isConnected()) {
                    socketClient.close();
                }

                socketClient = new DuelSocketClient();
                socketClient.connect(host, port, currentUsername, new DuelSocketClient.DuelListener() {
                    @Override
                    public void onOnlineUsers(List<String> onlineUsernames) {
                        onlineUsers.clear();
                        onlineUsers.addAll(onlineUsernames);
                        if (activeListener != null) {
                            activeListener.onOnlineUsers(onlineUsernames);
                        }
                    }

                    @Override
                    public void onDuelInvite(String fromUser, String constellation) {
                        if (activeListener != null) {
                            activeListener.onDuelInvite(fromUser, constellation);
                        }
                    }

                    @Override
                    public void onDuelAccepted(String fromUser, String constellation) {
                        if (activeListener != null) {
                            activeListener.onDuelAccepted(fromUser, constellation);
                        }
                    }

                    @Override
                    public void onDuelDeclined(String fromUser) {
                        if (activeListener != null) {
                            activeListener.onDuelDeclined(fromUser);
                        }
                    }

                    @Override
                    public void onRemoteLink(String fromUser, int starA, int starB) {
                        if (activeListener != null) {
                            activeListener.onRemoteLink(fromUser, starA, starB);
                        }
                    }

                    @Override
                    public void onRematch(String fromUser) {
                        if (activeListener != null) {
                            activeListener.onRematch(fromUser);
                        }
                    }

                    @Override
                    public void onOpponentLeft(String fromUser) {
                        if (activeListener != null) {
                            activeListener.onOpponentLeft(fromUser);
                        }
                    }

                    @Override
                    public void onDisconnected() {
                        onlineUsers.clear();
                        if (activeListener != null) {
                            activeListener.onDisconnected();
                        }
                    }
                });
            } catch (IOException e) {
                // DuelServer might not be running yet; quiet fallback without crashing
                System.out.println("[OnlineSessionManager] Server not available at " + DuelServer.DEFAULT_PORT + ": " + e.getMessage());
            } finally {
                connecting = false;
            }
        });

        connectThread.setDaemon(true);
        connectThread.start();
    }

    public synchronized void setActiveListener(DuelSocketClient.DuelListener listener) {
        this.activeListener = listener;
        if (listener != null && !onlineUsers.isEmpty()) {
            Platform.runLater(() -> listener.onOnlineUsers(new ArrayList<>(onlineUsers)));
        }
    }

    public synchronized void removeActiveListener(DuelSocketClient.DuelListener listener) {
        if (this.activeListener == listener) {
            this.activeListener = null;
        }
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public List<String> getOnlineUsers() {
        return Collections.unmodifiableList(onlineUsers);
    }

    public boolean isConnected() {
        return socketClient != null && socketClient.isConnected();
    }

    public void sendInvite(String toUser, String constellation) {
        if (socketClient != null && socketClient.isConnected()) {
            socketClient.sendInvite(toUser, constellation);
        }
    }

    public void acceptInvite(String toUser, String constellation) {
        if (socketClient != null && socketClient.isConnected()) {
            socketClient.acceptInvite(toUser, constellation);
        }
    }

    public void declineInvite(String toUser) {
        if (socketClient != null && socketClient.isConnected()) {
            socketClient.declineInvite(toUser);
        }
    }

    public void sendLink(String toUser, int starA, int starB) {
        if (socketClient != null && socketClient.isConnected()) {
            socketClient.sendLink(toUser, starA, starB);
        }
    }

    public void sendRematch(String toUser) {
        if (socketClient != null && socketClient.isConnected()) {
            socketClient.sendRematch(toUser);
        }
    }

    public void sendLeave(String toUser) {
        if (socketClient != null && socketClient.isConnected()) {
            socketClient.sendLeave(toUser);
        }
    }

    public void disconnect() {
        if (socketClient != null) {
            socketClient.close();
        }
        onlineUsers.clear();
    }
}
