// backend/src/main/java/com/example/AppGYM/controller/PhotoController.java
package com.example.AppGYM.controller;

import com.example.AppGYM.model.ProgressPhoto;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.ProgressPhotoRepository;
import com.example.AppGYM.service.StorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * Sube fotos de progreso (multipart/form-data).
 *
 * ● Registra trazas DEBUG detalladas para diagnosticar problemas 403 / CORS.
 */
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
@Slf4j
public class PhotoController {

    private final ProgressPhotoRepository repo;
    private final StorageService          storage; // S3 / Cloudinary / FS

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public void upload(@AuthenticationPrincipal User u,
                       @RequestParam ProgressPhoto.Type type,
                       @RequestPart MultipartFile file) {

        log.debug("PHOTO | user={} type={} size={}-bytes ct={}",
                u == null ? "null" : u.getEmail(),
                type, file.getSize(), file.getContentType());

        if (u == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No JWT");

        if (file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío");

        /* ---------- almacena fisicamente ---------- */
        String url = storage.save(file);   // <-- tu implementación

        /* ---------- persiste row ---------- */
        ProgressPhoto p = new ProgressPhoto();
        p.setUser(u);
        p.setDate(LocalDate.now());
        p.setType(type);
        p.setUrl(url);
        repo.save(p);

        /* --------- opcional: atajo en User --------- */
        switch (type) {
            case FRONT -> u.setFrontImgUrl(url);
            case SIDE  -> u.setSideImgUrl(url);
            case BACK  -> u.setBackImgUrl(url);
        }

        log.debug("PHOTO | saved OK  url={}", url);
    }
}
