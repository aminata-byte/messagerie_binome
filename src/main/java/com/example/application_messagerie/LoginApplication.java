package com.example.application_messagerie;

import com.example.application_messagerie.utils.JPAUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen; // ← ajouté : pour récupérer la taille de l'écran
import javafx.stage.Stage;

import javax.persistence.EntityManager;
import java.io.IOException;

public class LoginApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // ── Test de connexion à la base de données ─────────────────────────
        testerConnexionBDD();

        // ── Récupération de la taille de l'écran ───────────────────────────
        // ajouté : on récupère la largeur et hauteur réelles de l'écran
        Screen screen = Screen.getPrimary();
        double width = screen.getBounds().getWidth();
        double height = screen.getBounds().getHeight();

        // ── Chargement de l'interface ──────────────────────────────────────
        FXMLLoader fxmlLoader = new FXMLLoader(
                LoginApplication.class.getResource("Login.fxml")
        );

        // ajouté : la scène prend toute la taille de l'écran
        Scene scene = new Scene(fxmlLoader.load(), width, height);
        stage.setTitle("Messagerie Interne");
        stage.setScene(scene);
        stage.setMaximized(true); // ← ajouté : fenêtre en plein écran
        stage.show();
    }

    private void testerConnexionBDD() {
        try {
            EntityManager em = JPAUtil.getEntityManager();
            em.close();
            System.out.println(" Connexion à la base de données réussie !");
        } catch (Exception e) {
            System.err.println(" Erreur de connexion à la base de données : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
//
//         Ce qui a été ajouté — résumé :
//        ```
//        import Screen          → pour récupérer la taille de l'écran
//        Screen.getPrimary()    → récupère l'écran principal
//        getBounds()            → donne la largeur et hauteur réelles
//    new Scene(..., width, height) → scène adaptée à l'écran
//         setMaximized(true)     → fenêtre en plein écran au lancement