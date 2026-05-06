package com.mail.client;

import com.mail.client.model.ClientModel;
import com.mail.client.view.ClientViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {

    private ClientViewController viewController;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("view/client-view.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);

        viewController = loader.getController();
        ClientModel model = new ClientModel();
        viewController.initModel(model);

        stage.setTitle("Mail Client");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            viewController.shutdown();
            System.exit(0);
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
