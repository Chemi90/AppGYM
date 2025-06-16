// backend/src/main/java/com/example/AppGYM/service/StorageService.java
package com.example.AppGYM.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {

    /** Guarda la imagen en disco local (./uploads) y devuelve la URL pública simulada. */
    public String save(MultipartFile file){
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get("uploads");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path dst = dir.resolve(file.getOriginalFilename());
            file.transferTo(dst);
            // en producción devolverías la URL real (S3, Cloudinary, etc.)
            return "/uploads/"+file.getOriginalFilename();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
