package com.webserver.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import javax.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileService.class);
    @Value("${document.root:./documents}")
    private String documentRoot;
    @PostConstruct
    public void init() {
        try {
            Path rootPath = Paths.get(documentRoot);
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
            }
            String url = "https://www.scu.edu";
            String savePath = rootPath.resolve("index.html").toString();
            try {
                Process process = new ProcessBuilder("wget", "-O", savePath, url).start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    System.out.println("File downloaded successfully by wget");
                } else {
                    log.error("wget failed with exit code: {}", exitCode);
                }
            } catch (Exception e) {
                log.error("Failed to download index.html with wget: {}", e.getMessage());
            }
        } catch (IOException e) {
            log.error("Failed to create document root directory: {}", e.getMessage());
        }
    }

   
} 