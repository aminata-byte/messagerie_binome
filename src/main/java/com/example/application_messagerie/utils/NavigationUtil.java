package com.example.application_messagerie.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationUtil {

    public static void navigateTo(Stage stage, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationUtil.class.getResource(fxmlPath)
            );
            stage.hide();                         // ✅ Cacher avant tout changement
            stage.setMaximized(false);            // Reset silencieux (fenêtre cachée)
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
            stage.setMaximized(true);             // Maximiser avant d'afficher
            stage.show();                         // ✅ Afficher déjà maximisé
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}