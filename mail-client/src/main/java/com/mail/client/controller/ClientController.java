package com.mail.client.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mail.client.model.ClientModel;
import com.mail.shared.model.Email;
import com.mail.shared.protocol.LocalDateTimeAdapter;
import com.mail.shared.protocol.Request;
import com.mail.shared.protocol.Response;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Client controller: handles all communication with the server.
 * Opens a new socket for each operation (as required by specs).
 */
public class ClientController {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;
    private static final int POLL_INTERVAL_SECONDS = 5;

    private final ClientModel model;
    private final Gson gson;
    private ScheduledExecutorService pollScheduler;
    private Consumer<List<Email>> onNewEmailsCallback;

    public ClientController(ClientModel model) {
        this.model = model;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    /**
     * Set a callback to be invoked when new emails arrive (for notifications).
     */
    public void setOnNewEmails(Consumer<List<Email>> callback) {
        this.onNewEmailsCallback = callback;
    }

    /**
     * Attempt login. Runs in background thread.
     */
    public void login(String email, Consumer<Boolean> onResult) {
        runAsync(() -> {
            Response response = sendRequest(Request.login(email));
            Platform.runLater(() -> {
                if (response != null && response.getStatus() == Response.Status.OK) {
                    model.setUserEmail(email);
                    model.setConnected(true);
                    model.setStatusMessage("Connected as " + email);
                    onResult.accept(true);
                    startPolling();
                } else {
                    model.setConnected(false);
                    String msg = response != null ? response.getMessage() : "Cannot connect to server";
                    model.setStatusMessage(msg);
                    onResult.accept(false);
                }
            });
        });
    }

    /**
     * Fetch new emails from server.
     */
    public void fetchEmails() {
        runAsync(() -> {
            Response response = sendRequest(
                    Request.fetch(model.getUserEmail(), model.getLastFetchTimestamp()));
            Platform.runLater(() -> {
                if (response != null && response.getStatus() == Response.Status.OK) {
                    model.setConnected(true);
                    if (response.getEmails() != null && !response.getEmails().isEmpty()) {
                        model.addEmails(response.getEmails());
                        if (onNewEmailsCallback != null) {
                            onNewEmailsCallback.accept(response.getEmails());
                        }
                    }
                } else if (response == null) {
                    model.setConnected(false);
                    model.setStatusMessage("Connection lost - retrying...");
                }
            });
        });
    }

    /**
     * Send an email via the server.
     */
    public void sendEmail(Email email, Consumer<Response> onResult) {
        runAsync(() -> {
            Response response = sendRequest(Request.send(model.getUserEmail(), email));
            Platform.runLater(() -> {
                if (response != null) {
                    model.setConnected(true);
                    onResult.accept(response);
                } else {
                    model.setConnected(false);
                    model.setStatusMessage("Cannot connect to server");
                    onResult.accept(Response.error("Cannot connect to server"));
                }
            });
        });
    }

    /**
     * Delete an email from the server.
     */
    public void deleteEmail(Email email, Consumer<Response> onResult) {
        runAsync(() -> {
            Response response = sendRequest(
                    Request.delete(model.getUserEmail(), email.getId()));
            Platform.runLater(() -> {
                if (response != null && response.getStatus() == Response.Status.OK) {
                    model.setConnected(true);
                    model.removeEmail(email);
                    onResult.accept(response);
                } else if (response != null) {
                    onResult.accept(response);
                } else {
                    model.setConnected(false);
                    model.setStatusMessage("Cannot connect to server");
                    onResult.accept(Response.error("Cannot connect to server"));
                }
            });
        });
    }

    /**
     * Start periodic polling for new emails.
     */
    private void startPolling() {
        if (pollScheduler != null) {
            pollScheduler.shutdown();
        }
        pollScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "email-poll");
            t.setDaemon(true);
            return t;
        });
        // Initial fetch immediately, then periodic
        pollScheduler.scheduleAtFixedRate(this::fetchEmails, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stopPolling() {
        if (pollScheduler != null) {
            pollScheduler.shutdown();
        }
    }

    /**
     * Send a request to the server and return the response.
     * Opens a new socket each time (HTTP-style, as required).
     */
    private Response sendRequest(Request request) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            out.println(gson.toJson(request));
            String responseJson = in.readLine();
            return gson.fromJson(responseJson, Response.class);

        } catch (IOException e) {
            return null; // Connection failed
        }
    }

    private void runAsync(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
