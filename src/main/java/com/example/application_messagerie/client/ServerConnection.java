package com.example.application_messagerie.client;

import com.example.application_messagerie.protocol.Packet;
import com.example.application_messagerie.protocol.PacketType;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ServerConnection {

    private static final String HOST = "localhost";
    private static final int PORT = 5555;

    private static ServerConnection instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    // Callback appelé quand un paquet arrive du serveur
    private Consumer<Packet> onPacketReceived;

    private ServerConnection() {}

    // Singleton
    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    // Connexion au serveur
    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread d'écoute des messages entrants
            Thread listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("[CLIENT] Connecté au serveur.");
            return true;

        } catch (IOException e) {
            System.err.println("[CLIENT] Impossible de se connecter : " + e.getMessage());
            return false;
        }
    }

    // Écoute des messages du serveur
    private void listenForMessages() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Packet packet = Packet.fromJson(line);
                if (onPacketReceived != null) {
                    onPacketReceived.accept(packet);
                }
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Connexion perdue : " + e.getMessage());
            // RG10 : notifier perte de connexion
            if (onPacketReceived != null) {
                Packet errorPacket = new Packet(PacketType.ERROR, "Connexion perdue avec le serveur.");
                onPacketReceived.accept(errorPacket);
            }
        }
    }

    // Envoyer un paquet au serveur
    public void send(Packet packet) {
        try {
            if (out != null) {
                out.println(packet.toJson());
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Erreur envoi : " + e.getMessage());
        }
    }

    // LOGIN
    public void login(String username, String password) {
        Packet packet = new Packet(PacketType.LOGIN, username + ":" + password);
        send(packet);
    }

    // REGISTER
    public void register(String username, String password) {
        Packet packet = new Packet(PacketType.REGISTER, username + ":" + password);
        send(packet);
    }

    // SEND MESSAGE
    public void sendMessage(String receiver, String contenu) {
        Packet packet = new Packet(PacketType.SEND_MESSAGE, username, receiver, contenu);
        send(packet);
    }

    // GET USERS
    public void getUsers() {
        Packet packet = new Packet(PacketType.GET_USERS);
        send(packet);
    }

    // GET HISTORY
    public void getHistory(String otherUser) {
        Packet packet = new Packet(PacketType.GET_HISTORY, otherUser);
        send(packet);
    }

    // LOGOUT
    public void logout() {
        Packet packet = new Packet(PacketType.LOGOUT);
        send(packet);
        disconnect();
    }

    // Déconnexion
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // ignore
        }
        instance = null;
    }

    // Vérifier si connecté
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // Getters et Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public void setOnPacketReceived(Consumer<Packet> callback) {
        this.onPacketReceived = callback;
    }
}