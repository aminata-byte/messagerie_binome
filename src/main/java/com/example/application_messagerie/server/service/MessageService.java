package com.example.application_messagerie.server.service;

import com.example.application_messagerie.entity.Message;
import com.example.application_messagerie.entity.User;
import com.example.application_messagerie.server.repository.MessageRepository;
import com.example.application_messagerie.server.repository.UserRepository;

import java.util.List;

public class MessageService {

    private final MessageRepository messageRepository = new MessageRepository();
    private final UserRepository userRepository = new UserRepository();

    // Envoyer un message (RG5 + RG7)
    public String sendMessage(String senderUsername, String receiverUsername, String contenu) {

        // RG7 : contenu non vide
        if (contenu == null || contenu.trim().isEmpty()) {
            return "ERROR:Le message ne peut pas être vide.";
        }

        // RG7 : max 1000 caractères
        if (contenu.length() > 1000) {
            return "ERROR:Message trop long (max 1000 caractères).";
        }

        // RG5 : destinataire doit exister
        User receiver = userRepository.findByUsername(receiverUsername);
        if (receiver == null) {
            return "ERROR:Destinataire introuvable.";
        }

        User sender = userRepository.findByUsername(senderUsername);
        if (sender == null) {
            return "ERROR:Expéditeur introuvable.";
        }

        // Sauvegarder le message
        Message message = new Message(sender, receiver, contenu.trim());
        messageRepository.save(message);

        System.out.println("[MSG] " + senderUsername + " → " + receiverUsername + " : " + contenu);
        return "SUCCESS:" + message.getId();
    }

    // Historique conversation (RG8)
    public List<Message> getConversation(String user1, String user2) {
        return messageRepository.findConversation(user1, user2);
    }

    // Messages non livrés (RG6)
    public List<Message> getUndelivered(String username) {
        return messageRepository.findUndelivered(username);
    }

    // Marquer comme reçu
    public void markAsReceived(Long messageId) {
        messageRepository.updateStatut(messageId, Message.Statut.RECU);
    }

    // Marquer comme lu
    public void markAsRead(Long messageId) {
        messageRepository.updateStatut(messageId, Message.Statut.LU);
    }
}