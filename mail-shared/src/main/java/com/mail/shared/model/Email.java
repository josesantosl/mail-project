package com.mail.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents an email message.
 * Shared between client and server.
 */
public class Email implements Serializable {

    private String id;
    private String sender;
    private List<String> recipients;
    private String subject;
    private String body;
    private LocalDateTime date;

    public Email() {
        this.id = UUID.randomUUID().toString();
        this.date = LocalDateTime.now();
    }

    public Email(String sender, List<String> recipients, String subject, String body) {
        this();
        this.sender = sender;
        this.recipients = recipients;
        this.subject = subject;
        this.body = body;
    }

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public List<String> getRecipients() { return recipients; }
    public void setRecipients(List<String> recipients) { this.recipients = recipients; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    @Override
    public String toString() {
        return "Email{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", recipients=" + recipients +
                ", subject='" + subject + '\'' +
                ", date=" + date +
                '}';
    }
}
