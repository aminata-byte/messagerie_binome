package com.example.application_messagerie.server.handler;

import com.example.application_messagerie.entity.Message;
import com.example.application_messagerie.protocol.Packet;
import com.example.application_messagerie.protocol.PacketType;
import com.example.application_messagerie.server.ServerMain;
import com.example.application_messagerie.server.service.AuthService;
import com.example.application_messagerie.server.service.MessageService;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private final AuthService authService = new AuthService();
    private final MessageService messageService = new MessageService();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String line;
            while ((line = in.readLine()) != null) {
                Packet packet = Packet.fromJson(line);
                handlePacket(packet);
            }

        } catch (Exception e) {
            System.out.println("[HANDLER] Client déconnecté : " + username);
        } finally {
            disconnect();
        }
    }

    private void handlePacket(Packet packet) throws Exception {
        switch (packet.getType()) {

            case LOGIN -> {
                // content = "username:password"
                String[] parts = packet.getContent().split(":", 2);
                String user = parts[0];
                String pass = parts[1];

                boolean dejaConnecte = ServerMain.connectedClients.containsKey(user);
                String result = authService.login(user, pass, dejaConnecte);

                if (result.startsWith("SUCCESS")) {
                    this.username = user;
                    ServerMain.connectedClients.put(username, this);

                    // Envoyer SUCCESS
                    Packet response = new Packet(PacketType.SUCCESS, username);
                    sendMessage(response.toJson());

                    // RG6 : livrer messages en attente
                    deliverPendingMessages();

                    // Notifier tous les clients
                    notifyUserStatusChange(username, "ONLINE");

                    // RG12 : journaliser
                    System.out.println("[LOG] Connexion : " + username);

                } else {
                    Packet response = new Packet(PacketType.ERROR, result.split(":", 2)[1]);
                    sendMessage(response.toJson());
                }
            }

            case REGISTER -> {
                String[] parts = packet.getContent().split(":", 2);
                String user = parts[0];
                String pass = parts[1];

                String result = authService.register(user, pass);

                if (result.startsWith("SUCCESS")) {
                    Packet response = new Packet(PacketType.SUCCESS, "Inscription réussie.");
                    sendMessage(response.toJson());
                    System.out.println("[LOG] Inscription : " + user);
                } else {
                    Packet response = new Packet(PacketType.ERROR, result.split(":", 2)[1]);
                    sendMessage(response.toJson());
                }
            }

            case SEND_MESSAGE -> {
                // RG2 : doit être authentifié
                if (username == null) {
                    sendMessage(new Packet(PacketType.ERROR, "Non authentifié.").toJson());
                    return;
                }

                String receiver = packet.getReceiver();
                String contenu = packet.getContent();

                // RG5 + RG7 : validation
                String result = messageService.sendMessage(username, receiver, contenu);

                if (result.startsWith("SUCCESS")) {
                    // Si destinataire connecté → livrer en temps réel
                    if (ServerMain.connectedClients.containsKey(receiver)) {
                        Packet msg = new Packet(PacketType.RECEIVE_MESSAGE, username, receiver, contenu);
                        ServerMain.sendToClient(receiver, msg.toJson());
                    }
                    sendMessage(new Packet(PacketType.SUCCESS, "Message envoyé.").toJson());
                    System.out.println("[LOG] Message : " + username + " → " + receiver);
                } else {
                    sendMessage(new Packet(PacketType.ERROR, result.split(":", 2)[1]).toJson());
                }
            }

            case GET_USERS -> {
                // Envoyer liste de tous les utilisateurs
                StringBuilder userList = new StringBuilder();
                ServerMain.connectedClients.keySet().forEach(u -> userList.append(u).append(","));
                Packet response = new Packet(PacketType.USER_LIST, userList.toString());
                sendMessage(response.toJson());
            }

            case GET_HISTORY -> {
                // content = username de l'autre utilisateur
                String otherUser = packet.getContent();
                List<Message> history = messageService.getConversation(username, otherUser);

                StringBuilder sb = new StringBuilder();
                for (Message m : history) {
                    sb.append(m.getSender().getUsername())
                            .append(":")
                            .append(m.getContenu())
                            .append("|");
                }
                Packet response = new Packet(PacketType.HISTORY_RESPONSE, sb.toString());
                sendMessage(response.toJson());
            }

            case LOGOUT -> {
                disconnect();
            }
        }
    }

    // RG6 : livrer messages en attente
    private void deliverPendingMessages() throws Exception {
        List<Message> pending = messageService.getUndelivered(username);
        for (Message m : pending) {
            Packet msg = new Packet(
                    PacketType.RECEIVE_MESSAGE,
                    m.getSender().getUsername(),
                    username,
                    m.getContenu()
            );
            sendMessage(msg.toJson());
            messageService.markAsReceived(m.getId());
        }
    }

    // Notifier changement de statut
    private void notifyUserStatusChange(String user, String status) throws Exception {
        Packet packet = new Packet(PacketType.USER_STATUS_CHANGE, user, status);
        ServerMain.broadcast(packet.toJson());
    }

    // Déconnexion (RG4 + RG12)
    private void disconnect() {
        if (username != null) {
            authService.logout(username);
            ServerMain.connectedClients.remove(username);
            try {
                notifyUserStatusChange(username, "OFFLINE");
            } catch (Exception e) {
                // ignore
            }
            System.out.println("[LOG] Déconnexion : " + username);
            username = null;
        }
        try {
            socket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    // Envoyer un message JSON au client
    public void sendMessage(String json) {
        out.println(json);
    }
}