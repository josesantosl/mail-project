package com.mail.client.view;

import com.mail.client.controller.ClientController;
import com.mail.client.model.ClientModel;
import com.mail.shared.model.Email;
import com.mail.shared.protocol.Response;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * FXML controller for the client view.
 */
public class ClientViewController {

    // Email regex pattern for validation
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Login
    @FXML private HBox loginBar;
    @FXML private TextField emailField;
    @FXML private Label statusLabel;

    // Main content
    @FXML private SplitPane mainContent;
    @FXML private ListView<Email> emailListView;

    // Detail view
    @FXML private VBox detailView;
    @FXML private Label detailFrom;
    @FXML private Label detailTo;
    @FXML private Label detailDate;
    @FXML private Label detailSubject;
    @FXML private TextArea detailBody;

    // Compose view
    @FXML private VBox composeView;
    @FXML private TextField composeTo;
    @FXML private TextField composeSubject;
    @FXML private TextArea composeBody;

    // Error
    @FXML private Label errorLabel;

    private ClientModel model;
    private ClientController controller;

    public void initModel(ClientModel model) {
        this.model = model;
        this.controller = new ClientController(model);

        // Bind inbox list to model
        emailListView.setItems(model.getInbox());

        // Custom cell factory to show sender + subject
        emailListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Email email, boolean empty) {
                super.updateItem(email, empty);
                if (empty || email == null) {
                    setText(null);
                } else {
                    setText(email.getSender() + " - " + email.getSubject()
                            + "  (" + email.getDate().format(DATE_FMT) + ")");
                }
            }
        });

        // When selection changes, show email detail
        emailListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showEmailDetail(newVal));

        // Bind connection status
        model.connectedProperty().addListener((obs, oldVal, newVal) -> {
            statusLabel.setText(newVal ? "Connected" : "Disconnected");
            statusLabel.setStyle(newVal ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        });

        // Notification callback for new emails
        controller.setOnNewEmails(emails -> {
            if (!emails.isEmpty()) {
                showInfo("You have " + emails.size() + " new email(s)!");
            }
        });
    }

    // --- Login ---

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        clearError();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Invalid email format. Please enter a valid email address.");
            return;
        }

        controller.login(email, success -> {
            if (success) {
                loginBar.setDisable(true);
                mainContent.setVisible(true);
                mainContent.setManaged(true);
            } else {
                showError(model.getStatusMessage());
            }
        });
    }

    // --- Email detail ---

    private void showEmailDetail(Email email) {
        showDetailView();
        if (email == null) {
            detailFrom.setText("");
            detailTo.setText("");
            detailDate.setText("");
            detailSubject.setText("");
            detailBody.setText("");
            return;
        }
        detailFrom.setText(email.getSender());
        detailTo.setText(String.join(", ", email.getRecipients()));
        detailDate.setText(email.getDate().format(DATE_FMT));
        detailSubject.setText(email.getSubject());
        detailBody.setText(email.getBody());
    }

    // --- Compose ---

    @FXML
    private void onCompose() {
        showComposeView();
        composeTo.setText("");
        composeSubject.setText("");
        composeBody.setText("");
    }

    @FXML
    private void onReply() {
        Email selected = emailListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showComposeView();
        composeTo.setText(selected.getSender());
        composeSubject.setText("Re: " + selected.getSubject());
        composeBody.setText("\n\n--- Original message ---\n" + selected.getBody());
    }

    @FXML
    private void onReplyAll() {
        Email selected = emailListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showComposeView();

        // All recipients + sender, excluding current user
        List<String> allRecipients = new java.util.ArrayList<>();
        allRecipients.add(selected.getSender());
        allRecipients.addAll(selected.getRecipients());
        String recipients = allRecipients.stream()
                .filter(r -> !r.equals(model.getUserEmail()))
                .distinct()
                .collect(Collectors.joining(", "));

        composeTo.setText(recipients);
        composeSubject.setText("Re: " + selected.getSubject());
        composeBody.setText("\n\n--- Original message ---\n" + selected.getBody());
    }

    @FXML
    private void onForward() {
        Email selected = emailListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showComposeView();
        composeTo.setText("");
        composeSubject.setText("Fwd: " + selected.getSubject());
        composeBody.setText("\n\n--- Forwarded message ---\n"
                + "From: " + selected.getSender() + "\n"
                + "To: " + String.join(", ", selected.getRecipients()) + "\n"
                + "Date: " + selected.getDate().format(DATE_FMT) + "\n\n"
                + selected.getBody());
    }

    @FXML
    private void onSend() {
        clearError();

        String toText = composeTo.getText().trim();
        if (toText.isEmpty()) {
            showError("Please enter at least one recipient.");
            return;
        }

        // Parse and validate recipients
        List<String> recipients = Arrays.stream(toText.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        for (String r : recipients) {
            if (!EMAIL_PATTERN.matcher(r).matches()) {
                showError("Invalid email address: " + r);
                return;
            }
        }

        Email email = new Email(
                model.getUserEmail(),
                recipients,
                composeSubject.getText().trim(),
                composeBody.getText()
        );

        controller.sendEmail(email, response -> {
            if (response.getStatus() == Response.Status.OK) {
                showInfo("Email sent successfully!");
                showDetailView();
            } else {
                showError(response.getMessage());
            }
        });
    }

    @FXML
    private void onCancelCompose() {
        showDetailView();
    }

    // --- Delete ---

    @FXML
    private void onDelete() {
        Email selected = emailListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an email to delete.");
            return;
        }
        controller.deleteEmail(selected, response -> {
            if (response.getStatus() == Response.Status.OK) {
                showEmailDetail(null);
            } else {
                showError(response.getMessage());
            }
        });
    }

    // --- View toggling ---

    private void showDetailView() {
        detailView.setVisible(true);
        detailView.setManaged(true);
        composeView.setVisible(false);
        composeView.setManaged(false);
    }

    private void showComposeView() {
        detailView.setVisible(false);
        detailView.setManaged(false);
        composeView.setVisible(true);
        composeView.setManaged(true);
    }

    // --- Error/Info display ---

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setText(msg);
    }

    private void showInfo(String msg) {
        errorLabel.setStyle("-fx-text-fill: green;");
        errorLabel.setText(msg);
    }

    private void clearError() {
        errorLabel.setText("");
    }

    /**
     * Called when the application is closing.
     */
    public void shutdown() {
        if (controller != null) {
            controller.stopPolling();
        }
    }
}
