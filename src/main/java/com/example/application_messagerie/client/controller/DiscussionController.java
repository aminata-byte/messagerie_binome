package com.example.application_messagerie.client.controller;

import com.example.application_messagerie.client.ServerConnection;
import com.example.application_messagerie.protocol.Packet;
import com.example.application_messagerie.utils.NavigationUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @FXML private VBox welcomeBox;
    @FXML private HBox inputBox;
    @FXML private StackPane headerAvatar;
    @FXML private Label headerAvatarLabel;
    @FXML private ImageView bgImageView;

    private String selectedUser = null;
    private ServerConnection connection;

    private final Map<Long, Label> statusLabels = new HashMap<>();
    private final Map<Long, Label> pendingLabels = new HashMap<>();
    private long tempIdCounter = -1;

    private static final String AVATAR_COLOR  = "#2C5F8A";
    private static final String CELL_SELECTED = "-fx-background-color: #1A3D63; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String CELL_HOVER    = "-fx-background-color: #132D4A; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String CELL_NORMAL   = "-fx-background-color: transparent; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        connection = ServerConnection.getInstance();
        currentUserLabel.setText("Connecté : " + connection.getUsername());

        connection.setOnPacketReceived(packet ->
                Platform.runLater(() -> handlePacket(packet))
        );

        connection.getUsers();

        userListView.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;" +
                        "-fx-selection-bar: transparent; -fx-selection-bar-non-focused: transparent;"
        );

        Platform.runLater(() -> {
            double maxScroll = scrollPane.getScene().getHeight() - 65 - 65;
            scrollPane.setMaxHeight(maxScroll);
            scrollPane.setPrefHeight(maxScroll);
            scrollPane.getScene().heightProperty().addListener((obs, old, newH) -> {
                double max = newH.doubleValue() - 65 - 65;
                scrollPane.setMaxHeight(max);
                scrollPane.setPrefHeight(max);
            });
        });

        // Charger l'image de fond
        try {
            var url = DiscussionController.class.getResource("/images/aurora.jpg");
            if (url != null) {
                Image img = new Image(url.toExternalForm(), true);
                img.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() >= 1.0) {
                        Platform.runLater(() -> {
                            bgImageView.setImage(img);
                            bgImageView.setPreserveRatio(false);
                            bgImageView.setOpacity(0.5);
                            StackPane parent = (StackPane) bgImageView.getParent();
                            bgImageView.fitWidthProperty().bind(parent.widthProperty());
                            bgImageView.fitHeightProperty().bind(parent.heightProperty());
                        });
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }

        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            scrollPane.lookup(".viewport").setStyle("-fx-background-color: transparent;");
        });

        userListView.setCellFactory(list -> new ListCell<String>() {
            @Override
            protected void updateItem(String user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle(CELL_NORMAL);
                    setOnMouseEntered(null);
                    setOnMouseExited(null);
                } else {
                    // Format: "username:ONLINE/OFFLINE:lastTime:unread:lastMsg"
                    String[] parts = user.split(":", 5);
                    String displayName = parts[0];
                    boolean isOnline = parts.length >= 2 && parts[1].equals("ONLINE");
                    long unread = parts.length >= 4 ? parseLong(parts[3]) : 0;
                    String lastMsg = parts.length >= 5 ? parts[4] : "";
                    if (lastMsg.length() > 25) lastMsg = lastMsg.substring(0, 25) + "...";

                    // Avatar
                    StackPane avatar = new StackPane();
                    avatar.setPrefSize(40, 40);
                    avatar.setMinSize(40, 40);
                    avatar.setMaxSize(40, 40);
                    avatar.setStyle("-fx-background-color: " + AVATAR_COLOR + "; -fx-background-radius: 50;");
                    Label initiale = new Label(String.valueOf(displayName.charAt(0)).toUpperCase());
                    initiale.setStyle("-fx-text-fill: #F6FAFD; -fx-font-size: 15px; -fx-font-weight: bold;");
                    avatar.getChildren().add(initiale);

                    // Container avatar + point vert
                    StackPane avatarContainer = new StackPane();
                    avatarContainer.setPrefSize(42, 42);
                    avatarContainer.setMinSize(42, 42);
                    avatarContainer.setMaxSize(42, 42);
                    if (isOnline) {
                        Circle dot = new Circle(5);
                        dot.setFill(Color.web("#4CAF50"));
                        dot.setStyle("-fx-effect: dropshadow(gaussian, #4CAF50, 4, 0.5, 0, 0);");
                        StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);
                        avatarContainer.getChildren().addAll(avatar, dot);
                    } else {
                        avatarContainer.getChildren().add(avatar);
                    }

                    // Nom + dernier message
                    Label nomLabel = new Label(displayName);
                    nomLabel.setStyle("-fx-text-fill: " + (isOnline ? "#F6FAFD" : "#B3CFE5") +
                            "; -fx-font-size: 13px; -fx-font-weight: bold;");

                    Label lastMsgLabel = new Label(lastMsg);
                    lastMsgLabel.setStyle("-fx-text-fill: #8AA8C4; -fx-font-size: 11px;");
                    lastMsgLabel.setMaxWidth(130);

                    VBox nameBox = new VBox(2, nomLabel, lastMsgLabel);
                    nameBox.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(nameBox, Priority.ALWAYS);

                    // Badge non lus
                    HBox rightBox = new HBox();
                    rightBox.setAlignment(Pos.CENTER_RIGHT);
                    rightBox.setMinWidth(30);
                    if (unread > 0) {
                        Label badge = new Label(String.valueOf(unread));
                        badge.setStyle(
                                "-fx-background-color: #4CAF50;" +
                                        "-fx-text-fill: #F6FAFD;" +
                                        "-fx-font-size: 10px;" +
                                        "-fx-font-weight: bold;" +
                                        "-fx-background-radius: 10;" +
                                        "-fx-padding: 2 6;"
                        );
                        rightBox.getChildren().add(badge);
                    }

                    HBox cellContent = new HBox(10, avatarContainer, nameBox, rightBox);
                    cellContent.setAlignment(Pos.CENTER_LEFT);
                    cellContent.setStyle("-fx-padding: 8 10;");
                    setGraphic(cellContent);
                    setText(null);

                    // Style sélection
                    boolean selected = displayName.equals(selectedUser);
                    setStyle(selected ? CELL_SELECTED : CELL_NORMAL);

                    // Effet hover
                    setOnMouseEntered(e -> {
                        if (!displayName.equals(selectedUser)) setStyle(CELL_HOVER);
                    });
                    setOnMouseExited(e -> {
                        if (!displayName.equals(selectedUser)) setStyle(CELL_NORMAL);
                        else setStyle(CELL_SELECTED);
                    });
                }
            }
        });

        userListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        String[] parts = newVal.split(":", 5);
                        selectedUser = parts[0];
                        boolean isOnline = parts.length >= 2 && parts[1].equals("ONLINE");

                        chatTargetLabel.setText(selectedUser);
                        statusTargetLabel.setText(isOnline ? "en ligne" : "hors ligne");

                        headerAvatar.setStyle("-fx-background-color: " + AVATAR_COLOR + "; -fx-background-radius: 19;");
                        headerAvatarLabel.setText(String.valueOf(selectedUser.charAt(0)).toUpperCase());

                        messagesBox.getChildren().clear();
                        statusLabels.clear();
                        pendingLabels.clear();

                        welcomeBox.setVisible(false);
                        welcomeBox.setManaged(false);
                        scrollPane.setVisible(true);
                        scrollPane.setManaged(true);
                        inputBox.setVisible(true);
                        inputBox.setManaged(true);

                        userListView.refresh();

                        Platform.runLater(() -> {
                            if (scrollPane.getScene() != null) {
                                double maxScroll = scrollPane.getScene().getHeight() - 65 - 65;
                                scrollPane.setMaxHeight(maxScroll);
                                scrollPane.setPrefHeight(maxScroll);
                            }
                            if (scrollPane.lookup(".viewport") != null) {
                                scrollPane.lookup(".viewport").setStyle("-fx-background-color: transparent;");
                            }
                        });

                        connection.markRead(selectedUser);
                        connection.getHistory(selectedUser);
                    }
                }
        );
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0; }
    }

    private void handlePacket(Packet packet) {
        switch (packet.getType()) {

            case USER_LIST -> {
                String content = packet.getContent();
                if (content == null || content.isEmpty()) return;

                // Parser toutes les entrées
                List<String[]> parsed = new ArrayList<>();
                for (String u : content.split(",")) {
                    if (u.isEmpty()) continue;
                    String[] parts = u.split(":", 5);
                    String uname = parts[0];
                    if (!uname.isEmpty() && !uname.equals(connection.getUsername())) {
                        parsed.add(parts);
                    }
                }

                // Trier par lastTime décroissant (plus récent en haut)
                parsed.sort((a, b) -> {
                    long timeA = a.length >= 3 ? parseLong(a[2]) : 0;
                    long timeB = b.length >= 3 ? parseLong(b[2]) : 0;
                    return Long.compare(timeB, timeA);
                });

                userListView.getItems().clear();
                for (String[] parts : parsed) {
                    userListView.getItems().add(String.join(":", parts));
                }
            }

            case RECEIVE_MESSAGE -> {
                String sender = packet.getSender();
                String contenu = packet.getContent();
                String extra = packet.getExtra();
                Long msgId = null;
                String statut = "RECU";
                if (extra != null && extra.contains(":")) {
                    String[] parts = extra.split(":");
                    try { msgId = Long.parseLong(parts[0]); } catch (Exception ignored) {}
                    statut = parts[1];
                }
                if (sender.equals(selectedUser) || sender.equals(connection.getUsername())) {
                    afficherMessage(sender, contenu, msgId, statut);
                    if (sender.equals(selectedUser)) {
                        connection.markRead(selectedUser);
                    }
                }
                // Rafraîchir la liste pour mettre à jour badge et tri
                connection.getUsers();
            }

            case HISTORY_RESPONSE -> {
                messagesBox.getChildren().clear();
                statusLabels.clear();
                pendingLabels.clear();
                String content = packet.getContent();
                if (content != null && !content.isEmpty()) {
                    String[] messages = content.split("\\|");
                    for (String msg : messages) {
                        if (!msg.isEmpty()) {
                            String[] parts = msg.split(":", 4);
                            if (parts.length >= 2) {
                                String sender = parts[0];
                                String contenu = parts[1];
                                String statut = parts.length >= 3 ? parts[2] : "ENVOYE";
                                Long msgId = null;
                                if (parts.length >= 4) {
                                    try { msgId = Long.parseLong(parts[3]); } catch (Exception ignored) {}
                                }
                                afficherMessage(sender, contenu, msgId, statut);
                            }
                        }
                    }
                }
            }

            case MESSAGES_READ -> {
                String content = packet.getContent();
                if (content == null) return;

                int lastColon = content.lastIndexOf(":");
                if (lastColon == -1) return;

                String statut = content.substring(lastColon + 1).trim();
                String idsPart = content.substring(0, lastColon);

                for (String idStr : idsPart.split(",")) {
                    idStr = idStr.trim();
                    if (idStr.isEmpty()) continue;
                    try {
                        Long id = Long.parseLong(idStr);
                        Label lbl = statusLabels.get(id);
                        if (lbl != null) {
                            updateStatusLabel(lbl, statut);
                        } else {
                            if (!pendingLabels.isEmpty()) {
                                Map.Entry<Long, Label> first = pendingLabels.entrySet().iterator().next();
                                Label pendingLbl = first.getValue();
                                Long tempId = first.getKey();
                                updateStatusLabel(pendingLbl, statut);
                                statusLabels.put(id, pendingLbl);
                                statusLabels.remove(tempId);
                                pendingLabels.remove(tempId);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                // Rafraîchir la liste après lecture
                connection.getUsers();
            }

            case USER_STATUS_CHANGE -> {
                connection.getUsers();
            }

            case ERROR -> {
                chatTargetLabel.setText("Connexion perdue !");
                chatTargetLabel.setStyle("-fx-text-fill: #F22727;");
            }
        }
    }

    private void updateStatusLabel(Label lbl, String statut) {
        switch (statut) {
            case "ENVOYE" -> {
                lbl.setText("✓");
                lbl.setStyle("-fx-text-fill: #B3CFE5; -fx-font-size: 11px;");
            }
            case "RECU" -> {
                lbl.setText("✓✓");
                lbl.setStyle("-fx-text-fill: #B3CFE5; -fx-font-size: 11px;");
            }
            case "LU" -> {
                lbl.setText("✓✓");
                lbl.setStyle("-fx-text-fill: #4A7FA7; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        }
    }

    private void afficherMessage(String sender, String contenu, Long msgId, String statut) {
        boolean isMe = sender.equals(connection.getUsername());

        HBox hbox = new HBox(8);
        hbox.setMaxWidth(Double.MAX_VALUE);
        hbox.setFillHeight(false);

        StackPane avatar = new StackPane();
        avatar.setPrefSize(32, 32);
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        avatar.setStyle("-fx-background-color: " + AVATAR_COLOR + "; -fx-background-radius: 50;");
        Label initiale = new Label(String.valueOf(sender.charAt(0)).toUpperCase());
        initiale.setStyle("-fx-text-fill: #F6FAFD; -fx-font-size: 11px; -fx-font-weight: bold;");
        avatar.getChildren().add(initiale);

        Label bubble = new Label(contenu);
        bubble.setWrapText(true);
        bubble.setMaxWidth(340);

        Label statusLabel = new Label();
        if (isMe) {
            updateStatusLabel(statusLabel, statut != null ? statut : "ENVOYE");
            if (msgId != null && msgId > 0) {
                statusLabels.put(msgId, statusLabel);
            } else {
                long tempId = tempIdCounter--;
                pendingLabels.put(tempId, statusLabel);
            }
        }

        if (isMe) {
            bubble.setStyle(
                    "-fx-background-color: #4A7FA7;" +
                            "-fx-text-fill: #F6FAFD;" +
                            "-fx-padding: 10 15;" +
                            "-fx-background-radius: 15 15 0 15;" +
                            "-fx-font-size: 13px;"
            );
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.setAlignment(Pos.CENTER_RIGHT);
            VBox bubbleBox = new VBox(2, bubble, statusLabel);
            bubbleBox.setAlignment(Pos.CENTER_RIGHT);
            hbox.getChildren().addAll(spacer, bubbleBox, avatar);
        } else {
            bubble.setStyle(
                    "-fx-background-color: rgba(26,61,99,0.88);" +
                            "-fx-text-fill: #F6FAFD;" +
                            "-fx-padding: 10 15;" +
                            "-fx-background-radius: 15 15 15 0;" +
                            "-fx-font-size: 13px;"
            );
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.getChildren().addAll(avatar, bubble, spacer);
        }

        hbox.setStyle("-fx-padding: 4 12;");
        messagesBox.getChildren().add(hbox);
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

        afficherMessage(connection.getUsername(), contenu, null, "ENVOYE");
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