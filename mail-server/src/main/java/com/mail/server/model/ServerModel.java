package com.mail.server.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mail.shared.model.Email;
import com.mail.shared.protocol.LocalDateTimeAdapter;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server model: manages all mailboxes, persistence, and logging.
 */
public class ServerModel {

    private static final String DATA_DIR = "data";
    private final Set<String> registeredAccounts;
    private final ConcurrentHashMap<String, List<Email>> mailboxes;
    private final ObservableList<String> log;
    private final Gson gson;

    public ServerModel(List<String> accounts) {
        this.registeredAccounts = ConcurrentHashMap.newKeySet();
        this.registeredAccounts.addAll(accounts);
        this.mailboxes = new ConcurrentHashMap<>();
        this.log = FXCollections.observableArrayList();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();

        // Ensure data directory exists
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            addLog("ERROR: Could not create data directory: " + e.getMessage());
        }

        // Load existing mailboxes from disk
        for (String account : accounts) {
            mailboxes.put(account, loadMailbox(account));
        }
    }

    // --- Account management ---

    public boolean accountExists(String email) {
        return registeredAccounts.contains(email);
    }

    // --- Mailbox operations ---

    /**
     * Get emails for a user received after a given timestamp.
     */
    public synchronized List<Email> getEmailsSince(String user, LocalDateTime since) {
        List<Email> inbox = mailboxes.getOrDefault(user, Collections.emptyList());
        if (since == null) {
            return new ArrayList<>(inbox);
        }
        List<Email> newEmails = new ArrayList<>();
        for (Email e : inbox) {
            if (e.getDate().isAfter(since)) {
                newEmails.add(e);
            }
        }
        return newEmails;
    }

    /**
     * Deliver an email to a recipient's mailbox.
     */
    public synchronized void deliverEmail(String recipient, Email email) {
        mailboxes.computeIfAbsent(recipient, k -> new ArrayList<>());
        mailboxes.get(recipient).add(email);
        saveMailbox(recipient);
    }

    /**
     * Delete an email from a user's mailbox.
     */
    public synchronized boolean deleteEmail(String user, String emailId) {
        List<Email> inbox = mailboxes.get(user);
        if (inbox == null) return false;
        boolean removed = inbox.removeIf(e -> e.getId().equals(emailId));
        if (removed) {
            saveMailbox(user);
        }
        return removed;
    }

    // --- Persistence ---

    private List<Email> loadMailbox(String account) {
        Path path = Paths.get(DATA_DIR, account + ".json");
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<Email>>() {}.getType();
            List<Email> emails = gson.fromJson(reader, listType);
            return emails != null ? emails : new ArrayList<>();
        } catch (IOException e) {
            addLog("ERROR loading mailbox for " + account + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveMailbox(String account) {
        Path path = Paths.get(DATA_DIR, account + ".json");
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(mailboxes.get(account), writer);
        } catch (IOException e) {
            addLog("ERROR saving mailbox for " + account + ": " + e.getMessage());
        }
    }

    // --- Logging ---

    public void addLog(String entry) {
        String timestamp = LocalDateTime.now().toString();
        String logEntry = "[" + timestamp + "] " + entry;
        Platform.runLater(() -> log.add(logEntry));
    }

    public ObservableList<String> getLog() {
        return log;
    }
}
