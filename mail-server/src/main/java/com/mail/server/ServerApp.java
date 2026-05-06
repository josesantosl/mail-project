package com.mail.server;

import com.mail.server.model.ServerModel;
import com.mail.server.view.ServerViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

public class ServerApp extends Application {

    // Pre-configured accounts for the demo
    private static final List<String> ACCOUNTS = List.of(
            "jose@mail.com",
            "pepe@mail.com",
            "adrian@mail.com"
    );

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("view/server-view.fxml"));
        Scene scene = new Scene(loader.load(), 600, 400);

        ServerViewController viewController = loader.getController();
        ServerModel model = new ServerModel(ACCOUNTS);
        viewController.initModel(model);

        stage.setTitle("Mail Server");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
