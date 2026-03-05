package com.example.application_messagerie.server.repository;

import com.example.application_messagerie.entity.User;
import com.example.application_messagerie.utils.JPAUtil;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

public class UserRepository {

    // Sauvegarder un utilisateur
    public void save(User user) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // Trouver par username
    public User findByUsername(String username) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    // Vérifier si username existe déjà (RG1)
    public boolean existsByUsername(String username) {
        return findByUsername(username) != null;
    }

    // Mettre à jour le statut (RG4)
    public void updateStatus(String username, User.Status status) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery(
                            "UPDATE User u SET u.status = :status WHERE u.username = :username")
                    .setParameter("status", status)
                    .setParameter("username", username)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // Récupérer tous les utilisateurs
    public List<User> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u", User.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}