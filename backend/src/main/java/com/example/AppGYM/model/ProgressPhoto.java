// backend/src/main/java/com/example/AppGYM/model/ProgressPhoto.java
package com.example.AppGYM.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity @Data
public class ProgressPhoto {

    public enum Type { FRONT, SIDE, BACK }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(nullable=false)
    private User user;

    private LocalDate date;
    @Enumerated(EnumType.STRING)
    private Type type;

    /** URL en S3, Cloudinary, etc. */
    private String url;
}
