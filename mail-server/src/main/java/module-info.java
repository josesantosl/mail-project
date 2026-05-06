module com.mail.server {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires com.mail.shared;

    opens com.mail.server to javafx.fxml;
    opens com.mail.server.view to javafx.fxml;
    opens com.mail.server.model to com.google.gson;

    exports com.mail.server;
}
