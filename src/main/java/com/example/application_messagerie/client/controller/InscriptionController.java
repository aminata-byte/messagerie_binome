package com.example.application_messagerie.client.controller;

import com.example.application_messagerie.client.ServerConnection;
import com.example.application_messagerie.protocol.Packet;
import com.example.application_messagerie.utils.NavigationUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class InscriptionController {

    @FXML private Button btnRegister;
    @FXML private Button btnRetour;
    @FXML private PasswordField confirmPasswordField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private TextField usernameField;

    @FXML
    void onBackToLogin(ActionEvent event) {
        Stage stage = (Stage) btnRetour.getScene().getWindow();
        NavigationUtil.navigateTo(stage,
                "/com/example/application_messagerie/Login.fxml",
                "Messagerie Interne");
    }

    @FXML
    void onRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        // Vérifications locales
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #F22727;");
            statusLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        if (!password.equals(confirm)) {
            statusLabel.setStyle("-fx-text-fill: #F22727;");
            statusLabel.setText("Les mots de passe ne correspondent pas.");
            return;
        }

        if (password.length() < 4) {
            statusLabel.setStyle("-fx-text-fill: #F22727;");
            statusLabel.setText("Mot de passe trop court (min 4 caractères).");
            return;
        }

        // Connexion au serveur
        ServerConnection connection = ServerConnection.getInstance();
        if (!connection.isConnected()) {
            boolean connected = connection.connect();
            if (!connected) {
                statusLabel.setStyle("-fx-text-fill: #F22727;");
                statusLabel.setText("Impossible de se connecter au serveur.");
                return;
            }
        }

        // Écouter la réponse
        connection.setOnPacketReceived(packet ->
                Platform.runLater(() -> handleResponse(packet))
        );

        // Envoyer REGISTER
        statusLabel.setStyle("-fx-text-fill: orange;");
        statusLabel.setText("Inscription en cours...");
        connection.register(username, password);
    }

    private void handleResponse(Packet packet) {
        switch (packet.getType()) {
            case SUCCESS -> {
                statusLabel.setStyle("-fx-text-fill: #22c55e;");
                statusLabel.setText("Compte créé avec succès !");

                // Retour Login après 1 seconde
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        Platform.runLater(() -> {
                            Stage stage = (Stage) btnRegister.getScene().getWindow();
                            NavigationUtil.navigateTo(stage,
                                    "/com/example/application_messagerie/Login.fxml",
                                    "Messagerie Interne");
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
            case ERROR -> {
                statusLabel.setStyle("-fx-text-fill: #F22727;");
                statusLabel.setText(packet.getContent());
            }
        }
    }
}