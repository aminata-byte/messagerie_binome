package com.example.application_messagerie.client.controller;

import com.example.application_messagerie.client.ServerConnection;
import com.example.application_messagerie.protocol.Packet;
import com.example.application_messagerie.protocol.PacketType;
import com.example.application_messagerie.utils.NavigationUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class DiscussionController {

    @FXML private Button btnDeconnexion;
    @FXML private Button btnEnvoyer;
    @FXML private Label chatTargetLabel;
    @FXML private Label currentUserLabel;
    @FXML private TextField messageField;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private Label statusTargetLabel;
    @FXML private ListView<String> userListView;

    private String selectedUser = null;
    private ServerConnection connection;

    @FXML
    public void initialize() {
        connection = ServerConnection.getInstance();

        // Afficher le nom de l'utilisateur connecté
        currentUserLabel.setText("Connecté : " + connection.getUsername());

        // Écouter les paquets entrants
        connection.setOnPacketReceived(packet ->
                Platform.runLater(() -> handlePacket(packet))
        );

        // Charger la liste des utilisateurs
        connection.getUsers();

        // Sélection d'un utilisateur dans la liste
        userListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        selectedUser = newVal;
                        chatTargetLabel.setText(newVal);
                        statusTargetLabel.setText("en ligne");
                        messagesBox.getChildren().clear();
                        // Charger historique
                        connection.getHistory(selectedUser);
                    }
                }
        );
    }

    private void handlePacket(Packet packet) {
        switch (packet.getType()) {

            case USER_LIST -> {
                // content = "user1,user2,user3,"
                userListView.getItems().clear();
                String content = packet.getContent();
                if (content != null && !content.isEmpty()) {
                    String[] users = content.split(",");
                    for (String u : users) {
                        if (!u.isEmpty() && !u.equals(connection.getUsername())) {
                            userListView.getItems().add(u);
                        }
                    }
                }
            }

            case RECEIVE_MESSAGE -> {
                String sender = packet.getSender();
                String contenu = packet.getContent();
                // Afficher seulement si c'est la conversation active
                if (sender.equals(selectedUser) || sender.equals(connection.getUsername())) {
                    afficherMessage(sender, contenu);
                }
            }

            case HISTORY_RESPONSE -> {
                // content = "sender:contenu|sender:contenu|..."
                messagesBox.getChildren().clear();
                String content = packet.getContent();
                if (content != null && !content.isEmpty()) {
                    String[] messages = content.split("\\|");
                    for (String msg : messages) {
                        if (!msg.isEmpty()) {
                            String[] parts = msg.split(":", 2);
                            if (parts.length == 2) {
                                afficherMessage(parts[0], parts[1]);
                            }
                        }
                    }
                }
            }

            case USER_STATUS_CHANGE -> {
                // Rafraîchir la liste
                connection.getUsers();
            }

            case ERROR -> {
                // RG10 : perte de connexion
                chatTargetLabel.setText("Connexion perdue !");
                chatTargetLabel.setStyle("-fx-text-fill: #F22727;");
            }
        }
    }

    private void afficherMessage(String sender, String contenu) {
        boolean isMe = sender.equals(connection.getUsername());

        HBox hbox = new HBox();
        hbox.setMaxWidth(Double.MAX_VALUE);

        Label bubble = new Label(contenu);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);

        if (isMe) {
            // Message envoyé → droite
            bubble.setStyle(
                    "-fx-background-color: #D99177;" +
                            "-fx-text-fill: #262626;" +
                            "-fx-padding: 10 15;" +
                            "-fx-background-radius: 15 15 0 15;" +
                            "-fx-font-size: 14px;"
            );
            hbox.setStyle("-fx-alignment: CENTER_RIGHT; -fx-padding: 3 10;");
        } else {
            // Message reçu → gauche
            bubble.setStyle(
                    "-fx-background-color: #1a1a2e;" +
                            "-fx-text-fill: #F2F2F2;" +
                            "-fx-padding: 10 15;" +
                            "-fx-background-radius: 15 15 15 0;" +
                            "-fx-font-size: 14px;" +
                            "-fx-border-color: #224037;" +
                            "-fx-border-radius: 15 15 15 0;"
            );
            hbox.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 3 10;");
        }

        hbox.getChildren().add(bubble);
        messagesBox.getChildren().add(hbox);

        // Scroll automatique vers le bas
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    @FXML
    void onEnvoyerMessage(ActionEvent event) {
        if (selectedUser == null) {
            chatTargetLabel.setText("Sélectionnez un utilisateur !");
            return;
        }

        String contenu = messageField.getText().trim();

        if (contenu.isEmpty()) return;

        // Afficher immédiatement
        afficherMessage(connection.getUsername(), contenu);

        // Envoyer au serveur
        connection.sendMessage(selectedUser, contenu);

        messageField.clear();
    }

    @FXML
    void onDeconnexion(ActionEvent event) {
        connection.logout();
        Stage stage = (Stage) btnDeconnexion.getScene().getWindow();
        NavigationUtil.navigateTo(stage,
                "/com/example/application_messagerie/Login.fxml",
                "Messagerie Interne");
    }
}