package com.example.application_messagerie;

import com.example.application_messagerie.utils.JPAUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.persistence.EntityManager;
import java.io.IOException;

public class LoginApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        testerConnexionBDD();

        FXMLLoader fxmlLoader = new FXMLLoader(
                LoginApplication.class.getResource("Login.fxml")
        );

        stage.setTitle("Messagerie Interne");
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.setMaximized(false);
        stage.setMaximized(true);
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