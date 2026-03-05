package com.example.application_messagerie.protocol;

public enum PacketType {

    // Authentification
    LOGIN,
    REGISTER,
    LOGOUT,

    // Réponses serveur
    SUCCESS,
    ERROR,

    // Messagerie
    SEND_MESSAGE,
    RECEIVE_MESSAGE,

    // Utilisateurs
    GET_USERS,
    USER_LIST,
    USER_STATUS_CHANGE,

    // Historique
    GET_HISTORY,
    HISTORY_RESPONSE,

    // Lu / Reçu
    MARK_READ,
    MESSAGES_READ
}