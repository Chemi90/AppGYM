package com.example.AppGYM.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "workoutDate"}))
@Getter @Setter
public class DailyEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate workoutDate;

    @ManyToOne(optional = false)
    private User user;

    /** machineId → weight map stored as JSON */
    @Column(columnDefinition = "json")
    private String details;
}