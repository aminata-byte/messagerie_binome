package com.example.application_messagerie.client.controller;

import com.example.application_messagerie.client.ServerConnection;
import com.example.application_messagerie.protocol.Packet;
import com.example.application_messagerie.utils.NavigationUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private Button btnCreerCompte;
    @FXML private Button btnSeConnecter;
    @FXML private PasswordField passwordField;
    @FXML private TextField usernameField;
    @FXML private Label statusLabel;

    @FXML
    void onLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #F22727;");
            statusLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        // Connexion au serveur
        ServerConnection connection = ServerConnection.getInstance();

        if (!connection.isConnected()) {
            boolean connected = connection.connect();
            if (!connected) {
                statusLabel.setText("Impossible de se connecter au serveur.");
                statusLabel.setStyle("-fx-text-fill: #F22727;");
                return;
            }
        }

        // Écouter la réponse
        connection.setOnPacketReceived(packet -> {
            Platform.runLater(() -> handleLoginResponse(packet, username));
        });

        // Envoyer LOGIN
        statusLabel.setText("Connexion en cours...");
        statusLabel.setStyle("-fx-text-fill: orange;");
        connection.login(username, password);
    }

    private void handleLoginResponse(Packet packet, String username) {
        switch (packet.getType()) {
            case SUCCESS -> {
                ServerConnection.getInstance().setUsername(username);
                Stage stage = (Stage) btnSeConnecter.getScene().getWindow();
                NavigationUtil.navigateTo(stage,
                        "/com/example/application_messagerie/Discussion.fxml",
                        "Discussion");
            }
            case ERROR -> {
                statusLabel.setText(packet.getContent());
                statusLabel.setStyle("-fx-text-fill: #F22727;");
            }
        }
    }

    @FXML
    void onRegister(ActionEvent event) {
        Stage stage = (Stage) btnCreerCompte.getScene().getWindow();
        NavigationUtil.navigateTo(stage,
                "/com/example/application_messagerie/Inscription.fxml",
                "Inscription");
    }
}