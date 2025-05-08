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
            if (requestLine == null || (!requestLine.startsWith("GET "))) {
                sendError(out, "HTTP/1.1", 400, "Bad Request");
                return;
            }
            String[] parts = requestLine.split(" ");
            String rawPath = parts[1];
            String httpVersion = (parts.length > 2) ? parts[2] : "HTTP/1.0";
            boolean isHttp11 = httpVersion.equalsIgnoreCase("HTTP/1.1");
            boolean hasHost = false;
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("host:")) {
                    hasHost = true;
                }
            }
            if (isHttp11 && !hasHost) {
                sendError(out, httpVersion, 400, "Host header required");
                return;
            }
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
            if (!filePath.toAbsolutePath().normalize().startsWith(Paths.get(documentRoot).toAbsolutePath().normalize())) {
                sendError(out, httpVersion, 403, "Forbidden");
                return;
            }
            if (!Files.exists(filePath)) {
                sendError(out, httpVersion, 404, "Not Found");
                return;
            }
            if (!Files.isReadable(filePath)) {
                sendError(out, httpVersion, 403, "Forbidden");
                return;
            }
            String contentType = Files.probeContentType(filePath);
            byte[] content = Files.readAllBytes(filePath);
            String response = httpVersion + " 200 OK\r\n" +
                    "Content-Type: " + (contentType != null ? contentType : "application/octet-stream") + "\r\n" +
                    "Content-Length: " + content.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            out.write(response.getBytes());
            out.write(content);
        } catch (Exception e) {
           
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    private void sendError(OutputStream out, String proto, int code, String message) throws IOException {
        String reason;
        switch (code) {
            case 400: reason = "Bad Request"; break;
            case 403: reason = "Forbidden"; break;
            case 404: reason = "Not Found"; break;
            case 200: reason = "OK"; break;
            default: reason = message; break;
        }
        String response = proto + " " + code + " " + reason + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                reason;
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
