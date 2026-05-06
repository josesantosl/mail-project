package com.mail.server.view;

import com.mail.server.controller.ServerController;
import com.mail.server.model.ServerModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

/**
 * FXML controller for the server view.
 * Bridges the view (FXML) and the model via the ServerController.
 */
public class ServerViewController {

    @FXML private ListView<String> logListView;
    @FXML private Button startButton;
    @FXML private Button stopButton;

    private ServerModel model;
    private ServerController controller;
    private Thread serverThread;

    public void initModel(ServerModel model) {
        this.model = model;
        this.controller = new ServerController(model);

        // Bind log list to observable list in model (Observer pattern via JavaFX)
        logListView.setItems(model.getLog());

        // Auto-scroll to bottom when new log entry is added
        model.getLog().addListener(
            (javafx.collections.ListChangeListener.Change<? extends String> c) -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        logListView.scrollTo(model.getLog().size() - 1);
                    }
                }
            }
        );
    }

    @FXML
    private void onStart() {
        startButton.setDisable(true);
        stopButton.setDisable(false);

        serverThread = new Thread(() -> controller.start());
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @FXML
    private void onStop() {
        startButton.setDisable(false);
        stopButton.setDisable(true);
        controller.stop();
    }
}
