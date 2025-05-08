import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class WebMethod {
    public static final String DEFAULT_URL = "https://www.scu.edu";

    public static void downloadIndexHtml(String url, java.nio.file.Path rootPath, String savePath, int port, Socket clientSocket) {
        if (url == null) {
            url = DEFAULT_URL;
        }
        try {
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
            }
        } catch (IOException e) {
            sendMsgToUser(clientSocket, "Failed to create document root directory: " + e.getMessage());
            return;
        }
        boolean success = false;
        // 1. wget
        try {
            Process process = new ProcessBuilder(
                "wget", "-p", "-k", "-E", "-P", rootPath.toString(), url
            ).start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                sendMsgToUser(clientSocket, "File downloaded successfully by wget");
                success = true;
            } else {
                System.out.println("wget failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            System.out.println("Failed to download index.html with wget: " + e.getMessage());
        }
        // 2. PowerShell
        if (!success) {
            String command = String.format(
                "powershell.exe -Command \"Invoke-WebRequest -Uri '%s' -OutFile '%s'\"",
                url, savePath
            );
            try {
                Process process = Runtime.getRuntime().exec(command);
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    sendMsgToUser(clientSocket, "File downloaded successfully by PowerShell");
                    success = true;
                } else {
                    System.out.println("PowerShell download failed with exit code: " + exitCode);
                }
            } catch (Exception e) {
                System.out.println("Failed to download index.html with PowerShell: " + e.getMessage());
            }
        }
        // 3. Java openStream
        if (!success) {
            try (InputStream inputStream = java.net.URI.create(url).toURL().openStream()) {
                Files.copy(inputStream, rootPath.resolve("index.html"), StandardCopyOption.REPLACE_EXISTING);
                sendMsgToUser(clientSocket, "File downloaded successfully by Java openStream");
                success = true;
            } catch (IOException e) {
                System.out.println("Failed to download index.html with Java openStream: " + e.getMessage());
            }
        }
        if (!success) {
            sendMsgToUser(clientSocket, "All download methods failed. index.html was not downloaded.");
            System.out.println("All download methods failed. index.html was not downloaded.");
        }
        sendMsgToUser(clientSocket, "------------------------Document root is under: " + rootPath.toAbsolutePath().normalize() + "------------------------");
        sendMsgToUser(clientSocket, "------------------------You can access: http://localhost:" + port + "/");
    }

    private static void sendMsgToUser(Socket clientSocket, String msg) {
        if (clientSocket != null) {
            try {
                String response = "HTTP/1.0 200 OK\r\nContent-Type: text/plain\r\n\r\n" + msg + "\n";
                clientSocket.getOutputStream().write(response.getBytes());
            } catch (IOException e) {
                System.out.println(msg);
            }
        } else {
            System.out.println(msg);
        }
    }
}
