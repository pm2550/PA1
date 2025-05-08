package com.webserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.webserver.service.FileService;

@Configuration
public class ServerConfig {
    @Value("${document.root:./documents}")
    private String documentRoot;

    @Value("${server.port:8080}")
    private int port;

    @Bean
    public String getDocumentRoot() {
        return documentRoot;
    }

    @Bean
    public int getPort() {
        return port;
    }


    public FileService fileService() {
        return new FileService();
    }
}
