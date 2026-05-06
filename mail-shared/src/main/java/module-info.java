module com.mail.shared {
    requires com.google.gson;

    exports com.mail.shared.model;
    exports com.mail.shared.protocol;

    opens com.mail.shared.model to com.google.gson;
    opens com.mail.shared.protocol to com.google.gson;
}
