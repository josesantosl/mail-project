module com.mail.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires com.mail.shared;

    opens com.mail.client to javafx.fxml;
    opens com.mail.client.view to javafx.fxml;
    opens com.mail.client.model to com.google.gson;

    exports com.mail.client;
}
