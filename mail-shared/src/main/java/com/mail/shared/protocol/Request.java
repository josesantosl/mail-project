package com.mail.shared.protocol;

import com.mail.shared.model.Email;

/**
 * Represents a request from client to server.
 * Serialized as JSON for socket transmission.
 */
public class Request {

    public enum Type {
        LOGIN,          // Verify if email account exists
        FETCH,          // Fetch new emails since a given timestamp
        SEND,           // Send an email
        DELETE          // Delete an email by ID
    }

    private Type type;
    private String user;        // The logged-in user's email address
    private Email email;        // Used for SEND
    private String emailId;     // Used for DELETE
    private String since;       // ISO timestamp for FETCH (get emails after this time)

    public Request() {}

    public static Request login(String user) {
        Request r = new Request();
        r.type = Type.LOGIN;
        r.user = user;
        return r;
    }

    public static Request fetch(String user, String since) {
        Request r = new Request();
        r.type = Type.FETCH;
        r.user = user;
        r.since = since;
        return r;
    }

    public static Request send(String user, Email email) {
        Request r = new Request();
        r.type = Type.SEND;
        r.user = user;
        r.email = email;
        return r;
    }

    public static Request delete(String user, String emailId) {
        Request r = new Request();
        r.type = Type.DELETE;
        r.user = user;
        r.emailId = emailId;
        return r;
    }

    // --- Getters ---

    public Type getType() { return type; }
    public String getUser() { return user; }
    public Email getEmail() { return email; }
    public String getEmailId() { return emailId; }
    public String getSince() { return since; }
}
