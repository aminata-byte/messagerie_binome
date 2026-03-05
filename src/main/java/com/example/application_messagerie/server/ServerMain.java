package com.example.application_messagerie.server;

import com.example.application_messagerie.server.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMain {

    private static final int PORT = 5555;

    // Map des clients connectés : username → ClientHandler (RG3)
    public static final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("[SERVEUR] Démarrage sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVEUR] En attente de connexions...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVEUR] Nouveau client : " + clientSocket.getInetAddress());

                // RG11 : chaque client dans un thread séparé
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("[SERVEUR] Erreur : " + e.getMessage());
        }
    }

    // Envoyer un message à un client connecté
    public static void sendToClient(String username, String json) {
        ClientHandler handler = connectedClients.get(username);
        if (handler != null) {
            handler.sendMessage(json);
        }
    }

    // Diffuser à tous les clients connectés
    public static void broadcast(String json) {
        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(json);
        }
    }
}