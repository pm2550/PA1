package com.webserver.controller;

import com.webserver.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // @GetMapping("/")
    // public ResponseEntity<Resource> getIndex() {
    //     return fileService.getFile("index.html");
    // }

    // @GetMapping("/{filename:.+}")
    // public ResponseEntity<Resource> getFile(@PathVariable String filename) {
    //     return fileService.getFile(filename);
    // }
} 