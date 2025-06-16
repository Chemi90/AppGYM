package com.example.AppGYM.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Data
public class DailyEntry {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    private LocalDate date;

    /*  clave = machineId  |  valor = Exercise embebido  */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "daily_exercise")
    @MapKeyColumn(name = "machine_id")
    private Map<Long, Exercise> details = new HashMap<>();

    /* ───────── sub-documento embebido ───────── */
    @Embeddable @Data @EqualsAndHashCode(of = "name")
    public static class Exercise {
        private String  name;       // ← NUEVO (máquina “Prensa piernas” …)
        private Double  weightKg;
        private Integer reps;
        private Integer sets;
    }
}
