package com.example.application_messagerie.server.service;

import com.example.application_messagerie.entity.User;
import com.example.application_messagerie.server.repository.UserRepository;
import com.example.application_messagerie.utils.PasswordUtil;

public class AuthService {

    private final UserRepository userRepository = new UserRepository();

    // Inscription (RG1 + RG9)
    public String register(String username, String password) {
        // RG1 : username unique
        if (userRepository.existsByUsername(username)) {
            return "ERROR:Username déjà utilisé.";
        }

        // RG9 : mot de passe haché
        String hashedPassword = PasswordUtil.hash(password);
        User user = new User(username, hashedPassword);
        userRepository.save(user);

        System.out.println("[AUTH] Inscription réussie : " + username);
        return "SUCCESS:Inscription réussie.";
    }

    // Connexion (RG3 + RG4)
    public String login(String username, String password, boolean dejaConnecte) {
        // RG3 : déjà connecté
        if (dejaConnecte) {
            return "ERROR:Utilisateur déjà connecté.";
        }

        User user = userRepository.findByUsername(username);

        if (user == null) {
            return "ERROR:Utilisateur introuvable.";
        }

        // Vérifier mot de passe (RG9)
        if (!PasswordUtil.verify(password, user.getPassword())) {
            return "ERROR:Mot de passe incorrect.";
        }

        // RG4 : passer ONLINE
        userRepository.updateStatus(username, User.Status.ONLINE);

        System.out.println("[AUTH] Connexion réussie : " + username);
        return "SUCCESS:" + username;
    }

    // Déconnexion (RG4)
    public void logout(String username) {
        if (username != null) {
            userRepository.updateStatus(username, User.Status.OFFLINE);
            System.out.println("[AUTH] Déconnexion : " + username);
        }
    }

    // Récupérer un utilisateur
    public User getUser(String username) {
        return userRepository.findByUsername(username);
    }
}