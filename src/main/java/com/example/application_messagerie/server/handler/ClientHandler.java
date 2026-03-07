package com.example.application_messagerie.server.handler;

import com.example.application_messagerie.entity.Message;
import com.example.application_messagerie.entity.User;
import com.example.application_messagerie.protocol.Packet;
import com.example.application_messagerie.protocol.PacketType;
import com.example.application_messagerie.server.ServerMain;
import com.example.application_messagerie.server.service.AuthService;
import com.example.application_messagerie.server.service.MessageService;
import com.example.application_messagerie.server.repository.UserRepository;

import java.io.*;
import java.net.Socket;
import java.time.ZoneId;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private final AuthService authService = new AuthService();
    private final MessageService messageService = new MessageService();
    private final UserRepository userRepository = new UserRepository();

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
                String[] parts = packet.getContent().split(":", 2);
                String user = parts[0];
                String pass = parts[1];

                boolean dejaConnecte = ServerMain.connectedClients.containsKey(user);
                String result = authService.login(user, pass, dejaConnecte);

                if (result.startsWith("SUCCESS")) {
                    this.username = user;
                    ServerMain.connectedClients.put(username, this);
                    sendMessage(new Packet(PacketType.SUCCESS, username).toJson());
                    deliverPendingMessages();
                    notifyUserStatusChange(username, "ONLINE");
                    System.out.println("[LOG] Connexion : " + username);
                } else {
                    sendMessage(new Packet(PacketType.ERROR, result.split(":", 2)[1]).toJson());
                }
            }

            case REGISTER -> {
                String[] parts = packet.getContent().split(":", 2);
                String user = parts[0];
                String pass = parts[1];
                String result = authService.register(user, pass);
                if (result.startsWith("SUCCESS")) {
                    sendMessage(new Packet(PacketType.SUCCESS, "Inscription réussie.").toJson());
                    System.out.println("[LOG] Inscription : " + user);
                } else {
                    sendMessage(new Packet(PacketType.ERROR, result.split(":", 2)[1]).toJson());
                }
            }

            case SEND_MESSAGE -> {
                if (username == null) {
                    sendMessage(new Packet(PacketType.ERROR, "Non authentifié.").toJson());
                    return;
                }

                String receiver = packet.getReceiver();
                String contenu = packet.getContent();
                String result = messageService.sendMessage(username, receiver, contenu);

                if (result.startsWith("SUCCESS")) {
                    Long msgId = Long.parseLong(result.split(":")[1]);

                    if (ServerMain.connectedClients.containsKey(receiver)) {
                        messageService.markAsReceived(msgId);
                        Packet msg = new Packet(PacketType.RECEIVE_MESSAGE, username, receiver, contenu);
                        msg.setExtra(msgId + ":RECU");
                        ServerMain.sendToClient(receiver, msg.toJson());
                        Packet statusUpdate = new Packet(PacketType.MESSAGES_READ, null, null, msgId + ":RECU");
                        sendMessage(statusUpdate.toJson());
                        System.out.println("[DEBUG] MESSAGES_READ envoyé à " + username + " content=" + msgId + ":RECU");
                    } else {
                        Packet statusUpdate = new Packet(PacketType.MESSAGES_READ, null, null, msgId + ":ENVOYE");
                        sendMessage(statusUpdate.toJson());
                        System.out.println("[DEBUG] MESSAGES_READ envoyé à " + username + " content=" + msgId + ":ENVOYE");
                    }
                    System.out.println("[LOG] Message : " + username + " → " + receiver);
                } else {
                    sendMessage(new Packet(PacketType.ERROR, result.split(":", 2)[1]).toJson());
                }
            }

            case GET_USERS -> {
                List<User> allUsers = userRepository.findAll();
                StringBuilder userList = new StringBuilder();
                for (User u : allUsers) {
                    if (!u.getUsername().equals(username)) {
                        boolean isOnline = ServerMain.connectedClients.containsKey(u.getUsername());
                        Message lastMsg = messageService.getLastMessage(username, u.getUsername());
                        long unread = messageService.countUnread(username, u.getUsername());
                        String lastContent = "";
                        long lastTime = 0;
                        if (lastMsg != null) {
                            lastContent = lastMsg.getContenu().replace("|", " ").replace(",", " ");
                            // ✅ Correction : LocalDateTime → epoch milli
                            lastTime = lastMsg.getDateEnvoi()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli();
                        }
                        userList.append(u.getUsername())
                                .append(":").append(isOnline ? "ONLINE" : "OFFLINE")
                                .append(":").append(lastTime)
                                .append(":").append(unread)
                                .append(":").append(lastContent)
                                .append(",");
                    }
                }
                sendMessage(new Packet(PacketType.USER_LIST, userList.toString()).toJson());
            }

            case GET_HISTORY -> {
                String otherUser = packet.getContent();
                List<Message> history = messageService.getConversation(username, otherUser);
                StringBuilder sb = new StringBuilder();
                for (Message m : history) {
                    sb.append(m.getSender().getUsername())
                            .append(":").append(m.getContenu())
                            .append(":").append(m.getStatut().name())
                            .append(":").append(m.getId())
                            .append("|");
                }
                sendMessage(new Packet(PacketType.HISTORY_RESPONSE, sb.toString()).toJson());
            }

            case MARK_READ -> {
                String otherUser = packet.getContent();
                List<Long> ids = messageService.markConversationAsRead(username, otherUser);
                System.out.println("[DEBUG] MARK_READ de " + username + " pour " + otherUser + " ids=" + ids);
                if (!ids.isEmpty() && ServerMain.connectedClients.containsKey(otherUser)) {
                    String idList = ids.stream()
                            .map(String::valueOf)
                            .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b);
                    Packet notify = new Packet(PacketType.MESSAGES_READ, null, null, idList + ":LU");
                    ServerMain.sendToClient(otherUser, notify.toJson());
                }
            }

            case LOGOUT -> {
                disconnect();
            }
        }
    }

    private void deliverPendingMessages() throws Exception {
        List<Message> pending = messageService.getUndelivered(username);
        for (Message m : pending) {
            Packet msg = new Packet(PacketType.RECEIVE_MESSAGE,
                    m.getSender().getUsername(), username, m.getContenu());
            msg.setExtra(m.getId() + ":RECU");
            sendMessage(msg.toJson());
            messageService.markAsReceived(m.getId());
        }
    }

    private void notifyUserStatusChange(String user, String status) throws Exception {
        Packet packet = new Packet(PacketType.USER_STATUS_CHANGE, user, status);
        ServerMain.broadcast(packet.toJson());
    }

    private void disconnect() {
        if (username != null) {
            authService.logout(username);
            ServerMain.connectedClients.remove(username);
            try { notifyUserStatusChange(username, "OFFLINE"); } catch (Exception e) {}
            System.out.println("[LOG] Déconnexion : " + username);
            username = null;
        }
        try { socket.close(); } catch (IOException e) {}
    }

    public void sendMessage(String json) {
        out.println(json);
    }
}