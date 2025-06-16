// backend/src/main/java/com/example/AppGYM/model/BodyStats.java
package com.example.AppGYM.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity @Data
public class BodyStats {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(nullable = false)
    private User user;

    private LocalDate date;         // fecha del registro

    private Double weightKg;        // peso del día
    private Double neckCm, chestCm, waistCm, lowerAbsCm, hipCm,
            bicepsCm, bicepsFlexCm, forearmCm, thighCm, calfCm;

    private String frontImgUrl, sideImgUrl, backImgUrl;
}
