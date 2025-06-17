package com.example.AppGYM.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class StorageService {

    private final Path root = Path.of("uploads");           // crea carpeta local

    public String save(MultipartFile file) throws Exception {
        if (!Files.exists(root)) Files.createDirectories(root);

        String clean = file.getOriginalFilename()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        String name  = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + clean;

        Path target = root.resolve(name);
        Files.copy(file.getInputStream(), target);

        String url = "/uploads/" + name;                    // ajústalo a tu CDN/S3
        log.debug("STORAGE | saved {}", target.toAbsolutePath());
        return url;
    }
}
