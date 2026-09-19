package com.starlore.starlore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standalone Constellation Duel Socket Server.
 * Runs as a dedicated Java process (no JavaFX required).
 * 
 * Responsibilities:
 * 1. Accepts incoming player socket connections on port 5055.
 * 2. Authenticates players via HELLO:<username> handshake.
 * 3. Broadcasts real-time presence (ONLINE:<user1>,<user2>,...) to all connected clients.
 * 4. Routes duel challenges (INVITE, ACCEPT, DECLINE), real-time game moves (LINK),
 *    rematch requests, and disconnect notifications.
 */
public class DuelServer {

    public static final int DEFAULT_PORT = 5055;

    // Active client registry: username -> PrintWriter output stream
    private static final ConcurrentHashMap<String, PrintWriter> onlineClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("==================================================");
        System.out.println("  STARLORE: CONSTELLATION DUEL SERVER");
        System.out.println("  Status : ONLINE");
        System.out.println("  Port   : " + port);
        System.out.println("  Ready for stargazers to connect...");
        System.out.println("==================================================");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept(); // blocks until a client connects
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("[DuelServer] Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts the updated list of currently connected stargazers to everyone.
     */
    private static void broadcastOnlineList() {
        String list = String.join(",", onlineClients.keySet());
        String line = "ONLINE:" + list;
        for (PrintWriter pw : onlineClients.values()) {
            pw.println(line);
        }
    }

    /**
     * Sends a direct line to a specific user if they are currently online.
     */
    private static void sendTo(String toUser, String line) {
        PrintWriter recipientOut = onlineClients.get(toUser);
        if (recipientOut != null) {
            recipientOut.println(line);
        }
    }

    /**
     * Handles one connected stargazer for the entire lifetime of their connection.
     */
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private String username;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                // 1. Initial Handshake: Must begin with HELLO:<username>
                String handshake = in.readLine();
                if (handshake == null || !handshake.startsWith("HELLO:")) {
                    socket.close();
                    return;
                }

                username = handshake.substring("HELLO:".length()).trim();
                if (username.isEmpty()) {
                    socket.close();
                    return;
                }

                // Register this client
                onlineClients.put(username, out);
                System.out.println("[DuelServer] Stargazer connected: " + username + " | Online (" + onlineClients.size() + "): " + onlineClients.keySet());

                // Broadcast updated user presence to everyone
                broadcastOnlineList();

                // 2. Main Socket Message Loop
                String line;
                while ((line = in.readLine()) != null) {
                    int firstColon = line.indexOf(':');
                    if (firstColon < 0) continue;

                    String tag = line.substring(0, firstColon);
                    String rest = line.substring(firstColon + 1);

                    switch (tag) {
                        case "INVITE": {
                            // INVITE:<toUser>:<constellationName>
                            int secondColon = rest.indexOf(':');
                            if (secondColon < 0) continue;
                            String toUser = rest.substring(0, secondColon);
                            String constellation = rest.substring(secondColon + 1);
                            System.out.println("[DuelServer] Challenge: " + username + " -> " + toUser + " on " + constellation);
                            sendTo(toUser, "INVITE:" + username + ":" + constellation);
                            break;
                        }
                        case "ACCEPT": {
                            // ACCEPT:<toUser>:<constellationName>
                            int secondColon = rest.indexOf(':');
                            if (secondColon < 0) continue;
                            String toUser = rest.substring(0, secondColon);
                            String constellation = rest.substring(secondColon + 1);
                            System.out.println("[DuelServer] Challenge ACCEPTED: " + username + " accepted " + toUser + "'s challenge on " + constellation);
                            sendTo(toUser, "ACCEPT:" + username + ":" + constellation);
                            break;
                        }
                        case "DECLINE": {
                            // DECLINE:<toUser>
                            String toUser = rest.trim();
                            System.out.println("[DuelServer] Challenge DECLINED: " + username + " declined " + toUser);
                            sendTo(toUser, "DECLINE:" + username);
                            break;
                        }
                        case "LINK": {
                            // LINK:<toUser>:<starA>,<starB>
                            int secondColon = rest.indexOf(':');
                            if (secondColon < 0) continue;
                            String toUser = rest.substring(0, secondColon);
                            String coords = rest.substring(secondColon + 1);
                            System.out.println("[DuelServer] Move: " + username + " -> " + toUser + " [Link " + coords + "]");
                            sendTo(toUser, "LINK:" + username + ":" + coords);
                            break;
                        }
                        case "REMATCH": {
                            // REMATCH:<toUser>
                            String toUser = rest.trim();
                            System.out.println("[DuelServer] Rematch requested: " + username + " -> " + toUser);
                            sendTo(toUser, "REMATCH:" + username);
                            break;
                        }
                        case "LEAVE": {
                            // LEAVE:<toUser>
                            String toUser = rest.trim();
                            System.out.println("[DuelServer] Match abandoned: " + username + " left match with " + toUser);
                            sendTo(toUser, "LEAVE:" + username);
                            break;
                        }
                        default:
                            System.out.println("[DuelServer] Unknown protocol line from " + username + ": " + line);
                            break;
                    }
                }

            } catch (IOException e) {
                // Connection closed or dropped
            } finally {
                // Cleanup on disconnect
                if (username != null) {
                    onlineClients.remove(username);
                    System.out.println("[DuelServer] Stargazer disconnected: " + username + " | Online (" + onlineClients.size() + "): " + onlineClients.keySet());
                    broadcastOnlineList();
                }
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
