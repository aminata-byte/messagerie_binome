package com.example.application_messagerie.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // Hacher un mot de passe (RG9)
    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Vérifier un mot de passe
    public static boolean verify(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
