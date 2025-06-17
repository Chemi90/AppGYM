package com.example.AppGYM.controller;

import com.example.AppGYM.model.ProgressPhoto;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.ProgressPhotoRepository;
import com.example.AppGYM.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
@Slf4j
public class PhotoController {

    private final ProgressPhotoRepository repo;
    private final StorageService          storage;
    private final HttpServletRequest      request;   // para IP y cabeceras si las necesitas

    /* --------------------------------------------------------------------- */
    @PostMapping
    @Transactional
    public ResponseEntity<?> upload(@AuthenticationPrincipal User u,
                                    @RequestParam ProgressPhoto.Type type,
                                    @RequestPart MultipartFile file) {

        log.debug("PHOTO | user={} type={} size={}-bytes ct={}",
                u.getEmail(), type, file.getSize(), file.getContentType());

        try {
            /* ---- 1) sube a S3 / disco / Cloudinary ---- */
            String url = storage.save(file);               // tu StorageService

            /* ---- 2) persiste el registro ---- */
            ProgressPhoto p = new ProgressPhoto();
            p.setUser(u);
            p.setDate(LocalDate.now());
            p.setType(type);
            p.setUrl(url);
            repo.save(p);

            /* ---- 3) opcional: reflejar la URL “actual” en User ---- */
            switch (type) {
                case FRONT -> u.setFrontImgUrl(url);
                case SIDE  -> u.setSideImgUrl(url);
                case BACK  -> u.setBackImgUrl(url);
            }

            log.debug("PHOTO | saved OK  url={}", url);
            return ResponseEntity.ok().build();

        } catch (MaxUploadSizeExceededException ex) {
            log.warn("PHOTO | TOO BIG  size={} remote={}", file.getSize(), request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body("Archivo demasiado grande (" + file.getSize() + " B). Máx. 5 MB");
        } catch (Exception ex) {
            log.error("PHOTO | ERROR {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error almacenando la imagen");
        }
    }
}
