// backend/src/main/java/com/example/AppGYM/controller/PhotoController.java
package com.example.AppGYM.controller;

import com.example.AppGYM.model.ProgressPhoto;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.ProgressPhotoRepository;
import com.example.AppGYM.service.StorageService;       // ←  import faltante
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final ProgressPhotoRepository repo;
    private final StorageService storage;   // se inyecta sin problemas

    @PostMapping
    @Transactional
    public void upload(@AuthenticationPrincipal User u,
                       @RequestParam ProgressPhoto.Type type,
                       @RequestPart MultipartFile file) {

        String url = storage.save(file);      // almacenamiento externo/local

        ProgressPhoto p = new ProgressPhoto();
        p.setUser(u);
        p.setDate(LocalDate.now());
        p.setType(type);
        p.setUrl(url);
        repo.save(p);

        switch (type) {
            case FRONT -> u.setFrontImgUrl(url);
            case SIDE  -> u.setSideImgUrl(url);
            case BACK  -> u.setBackImgUrl(url);
        }
    }
}
