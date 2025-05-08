package com.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {
    private int port;
    private final String documentRoot;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public WebServer(int port, String documentRoot) {
        this.port = port;
        this.documentRoot = documentRoot;
    }

    public void start() throws IOException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Port " + port + " is in use");
            port++;
            while (serverSocket == null && port < 9999) {
                try {
                    serverSocket = new ServerSocket(port);
                } catch (IOException ex) {
                    port++;
                }
            }
        }
        if (serverSocket == null) {
            throw new IOException("Could not bind to any port.");
        }
        System.out.println("Server started on port " + port);
        WebMethod.downloadIndexHtml(null, Paths.get(documentRoot), documentRoot + "/index.html", port, null);
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } finally {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {
            String requestLine = in.readLine();
            if (requestLine == null || !requestLine.startsWith("GET ")) {
                sendError(out, 400, "Please use GET method");
                return;
            }
            // String[] parts = requestLine.split(" ");
            String rawPath = requestLine.split(" ")[1];
            String path = java.net.URLDecoder.decode(rawPath, java.nio.charset.StandardCharsets.UTF_8);
            if (path.equals("/") || path.isBlank()) {
                path = "index.html";
            } else {
                if (path.startsWith("/") || path.startsWith("\\")) {
                    path = path.substring(1); 
                }
            }
            Path filePath = Paths.get(documentRoot)
                     .resolve(path)
                     .normalize();
            System.out.println(path);
            System.out.println(filePath);
            if (!filePath.toAbsolutePath().normalize().startsWith(Paths.get(documentRoot).toAbsolutePath().normalize())) {
                sendError(out, 403, "No athorization to access this file");
                return;
            }
            if (!Files.exists(filePath)) {
                sendError(out, 404, "Not Found");
                return;
            }
            if (!Files.isReadable(filePath)) {
                sendError(out, 403, "Unreadable file");
                return;
            }
            String contentType = Files.probeContentType(filePath);
            byte[] content = Files.readAllBytes(filePath);
            String response = "HTTP/1.0 200 OK\r\n" +
                    "Content-Type: " + (contentType != null ? contentType : "application/octet-stream") + "\r\n" +
                    "Content-Length: " + content.length + "\r\n" +
                    "\r\n";
            out.write(response.getBytes());
            out.write(content);
        } catch (Exception e) {
           
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    private void sendError(OutputStream out, int code, String message) throws IOException {
        String response = "HTTP/1.0 " + code + " " + message + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                message;
        out.write(response.getBytes());
    }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        String docRoot = "./documents";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-port") && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
            } else if (args[i].equals("-document_root") && i + 1 < args.length) {
                docRoot = args[i + 1];
            }
        }
        new WebServer(port, docRoot).start();
    }
}
