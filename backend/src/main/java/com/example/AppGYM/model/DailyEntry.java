// backend/src/main/java/com/example/AppGYM/model/DailyEntry.java
package com.example.AppGYM.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Entity @Data
public class DailyEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(nullable=false)
    private User user;

    private LocalDate date;

    /** máquinaId -> peso/reps/sets */
    @ElementCollection
    private Map<Long,Exercise> details;

    @Embeddable @Data
    public static class Exercise {
        private Double weightKg;
        private Integer reps;
        private Integer sets;
    }
}
