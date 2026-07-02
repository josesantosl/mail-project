package com.mail.client.model;

import com.mail.shared.model.Email;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ClientModel {

    private final StringProperty userEmail = new SimpleStringProperty("");
    private final ObservableList<Email> inbox = FXCollections.observableArrayList();
    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("");

    // Track the latest email date we've seen, for incremental fetch
    private String lastFetchTimestamp = null;

    public StringProperty userEmailProperty() { return userEmail; }
    public String getUserEmail() { return userEmail.get(); }
    public void setUserEmail(String email) { userEmail.set(email); }

    public ObservableList<Email> getInbox() { return inbox; }

    public BooleanProperty connectedProperty() { return connected; }
    public boolean isConnected() { return connected.get(); }
    public void setConnected(boolean connected) { this.connected.set(connected); }

    public StringProperty statusMessageProperty() { return statusMessage; }
    public String getStatusMessage() { return statusMessage.get(); }
    public void setStatusMessage(String msg) { statusMessage.set(msg); }

    public String getLastFetchTimestamp() { return lastFetchTimestamp; }
    public void setLastFetchTimestamp(String ts) { this.lastFetchTimestamp = ts; }

    // --- Inbox operations ---

    public void addEmails(java.util.List<Email> emails) {
        inbox.addAll(0, emails);
        for (Email e : emails) {
            String ts = e.getDate().toString();
            if (lastFetchTimestamp == null || ts.compareTo(lastFetchTimestamp) > 0) {
                lastFetchTimestamp = ts;
            }
        }
    }

    public void removeEmail(Email email) {
        inbox.remove(email);
    }
}
