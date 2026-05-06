package com.mail.shared.protocol;

import com.mail.shared.model.Email;
import java.util.List;

/**
 * Represents a response from server to client.
 * Serialized as JSON for socket transmission.
 */
public class Response {

    public enum Status {
        OK,
        ERROR
    }

    private Status status;
    private String message;         // Human-readable message (or error description)
    private List<Email> emails;     // Used for FETCH responses

    public Response() {}

    public static Response ok(String message) {
        Response r = new Response();
        r.status = Status.OK;
        r.message = message;
        return r;
    }

    public static Response ok(String message, List<Email> emails) {
        Response r = new Response();
        r.status = Status.OK;
        r.message = message;
        r.emails = emails;
        return r;
    }

    public static Response error(String message) {
        Response r = new Response();
        r.status = Status.ERROR;
        r.message = message;
        return r;
    }

    // --- Getters ---

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public List<Email> getEmails() { return emails; }
}
