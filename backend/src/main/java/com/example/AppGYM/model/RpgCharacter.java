// backend/src/main/java/com/example/gymapp/model/RpgCharacter.java
package com.example.AppGYM.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class RpgCharacter {

    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false)         // Guerrero / Mago / etc.
    private String clazz;

    private int level  = 1;
    private int xp     = 0;
    private int xpNext = 100;

    /*  -------- Atributos principales --------  */
    private int str = 10;   // fuerza
    private int eng = 10;   // energía
    private int vit = 10;   // vitalidad
    private int wis = 10;   // sabiduría

    /*  Relación 1-1 con tu entidad User (suponiendo que ya existe)  */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}
