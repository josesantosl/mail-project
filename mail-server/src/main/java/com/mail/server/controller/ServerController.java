package com.mail.server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mail.shared.model.Email;
import com.mail.shared.protocol.LocalDateTimeAdapter;
import com.mail.shared.protocol.Request;
import com.mail.shared.protocol.Response;
import com.mail.server.model.ServerModel;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server controller: listens for connections and dispatches request handling to a thread pool.
 */
public class ServerController {

    private static final int PORT = 9000;
    private final ServerModel model;
    private final Gson gson;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;

    public ServerController(ServerModel model) {
        this.model = model;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    /**
     * Start listening for client connections.
     * Call this from a background thread.
     */
    public void start() {
        running = true;
        threadPool = Executors.newCachedThreadPool();

        try {
            serverSocket = new ServerSocket(PORT);
            model.addLog("Server started on port " + PORT);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleClient(clientSocket));
                } catch (SocketException e) {
                    if (running) {
                        model.addLog("Socket error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            model.addLog("Could not start server: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            model.addLog("Error closing server socket: " + e.getMessage());
        }
        if (threadPool != null) {
            threadPool.shutdown();
        }
        model.addLog("Server stopped");
    }

    private void handleClient(Socket socket) {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        model.addLog("Connection opened from " + clientAddr);

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            String requestJson = in.readLine();
            if (requestJson == null) {
                model.addLog("Empty request from " + clientAddr);
                return;
            }

            Request request = gson.fromJson(requestJson, Request.class);
            Response response = processRequest(request);
            out.println(gson.toJson(response));

        } catch (Exception e) {
            model.addLog("Error handling client " + clientAddr + ": " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            model.addLog("Connection closed from " + clientAddr);
        }
    }

    private Response processRequest(Request request) {
        return switch (request.getType()) {
            case LOGIN -> handleLogin(request);
            case FETCH -> handleFetch(request);
            case SEND -> handleSend(request);
            case DELETE -> handleDelete(request);
        };
    }

    private Response handleLogin(Request request) {
        String user = request.getUser();
        if (model.accountExists(user)) {
            model.addLog("LOGIN successful: " + user);
            return Response.ok("Login successful");
        } else {
            model.addLog("LOGIN failed: " + user + " (account not found)");
            return Response.error("Account does not exist: " + user);
        }
    }

    private Response handleFetch(Request request) {
        String user = request.getUser();
        if (!model.accountExists(user)) {
            return Response.error("Account does not exist: " + user);
        }

        LocalDateTime since = null;
        if (request.getSince() != null && !request.getSince().isEmpty()) {
            since = LocalDateTime.parse(request.getSince());
        }

        List<Email> emails = model.getEmailsSince(user, since);
        model.addLog("FETCH for " + user + ": " + emails.size() + " email(s)");
        return Response.ok("Fetched " + emails.size() + " email(s)", emails);
    }

    private Response handleSend(Request request) {
        Email email = request.getEmail();
        String sender = request.getUser();

        // Validate all recipients exist
        List<String> invalidRecipients = new ArrayList<>();
        for (String recipient : email.getRecipients()) {
            if (!model.accountExists(recipient)) {
                invalidRecipients.add(recipient);
            }
        }

        if (!invalidRecipients.isEmpty()) {
            String msg = "Recipients not found: " + String.join(", ", invalidRecipients);
            model.addLog("SEND from " + sender + " FAILED: " + msg);
            return Response.error(msg);
        }

        // Deliver to all recipients
        for (String recipient : email.getRecipients()) {
            model.deliverEmail(recipient, email);
        }

        model.addLog("SEND from " + sender + " to " + email.getRecipients() + " - Subject: " + email.getSubject());
        return Response.ok("Email sent successfully");
    }

    private Response handleDelete(Request request) {
        String user = request.getUser();
        String emailId = request.getEmailId();

        if (model.deleteEmail(user, emailId)) {
            model.addLog("DELETE by " + user + ": email " + emailId);
            return Response.ok("Email deleted");
        } else {
            model.addLog("DELETE by " + user + " FAILED: email " + emailId + " not found");
            return Response.error("Email not found");
        }
    }
}
