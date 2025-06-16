// backend/src/main/java/com/example/AppGYM/model/UserMachine.java
package com.example.AppGYM.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity @Data
public class UserMachine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(nullable=false)
    private User user;

    @ManyToOne @JoinColumn(nullable=false)
    private Machine machine;

    private Double weightKg;
    private Integer reps;
    private Integer sets;
}
