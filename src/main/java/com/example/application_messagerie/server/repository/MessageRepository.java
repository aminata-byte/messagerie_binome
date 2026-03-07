package com.example.application_messagerie.server.repository;

import com.example.application_messagerie.entity.Message;
import com.example.application_messagerie.utils.JPAUtil;

import javax.persistence.EntityManager;
import java.util.List;

public class MessageRepository {

    // Sauvegarder un message
    public void save(Message message) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(message);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // Historique entre 2 utilisateurs (RG8 - ordre chronologique)
    public List<Message> findConversation(String user1, String user2) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT m FROM Message m " +
                                    "WHERE (m.sender.username = :user1 AND m.receiver.username = :user2) " +
                                    "OR (m.sender.username = :user2 AND m.receiver.username = :user1) " +
                                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("user1", user1)
                    .setParameter("user2", user2)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Messages non livrés pour un utilisateur (RG6)
    public List<Message> findUndelivered(String username) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT m FROM Message m " +
                                    "WHERE m.receiver.username = :username " +
                                    "AND m.statut = :statut " +
                                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("username", username)
                    .setParameter("statut", Message.Statut.ENVOYE)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Mettre à jour le statut d'un message
    public void updateStatut(Long messageId, Message.Statut statut) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery(
                            "UPDATE Message m SET m.statut = :statut WHERE m.id = :id")
                    .setParameter("statut", statut)
                    .setParameter("id", messageId)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // Marquer tous les messages non lus comme LU et retourner leurs IDs
    public List<Long> markConversationAsRead(String receiverUsername, String senderUsername) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Long> ids = em.createQuery(
                            "SELECT m.id FROM Message m " +
                                    "WHERE m.receiver.username = :receiver " +
                                    "AND m.sender.username = :sender " +
                                    "AND m.statut != :statut", Long.class)
                    .setParameter("receiver", receiverUsername)
                    .setParameter("sender", senderUsername)
                    .setParameter("statut", Message.Statut.LU)
                    .getResultList();

            if (!ids.isEmpty()) {
                em.getTransaction().begin();
                em.createQuery(
                                "UPDATE Message m SET m.statut = :statut " +
                                        "WHERE m.id IN :ids")
                        .setParameter("statut", Message.Statut.LU)
                        .setParameter("ids", ids)
                        .executeUpdate();
                em.getTransaction().commit();
            }
            return ids;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}