package com.example.AppGYM.repository;

import com.example.AppGYM.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {
    /** Busca una máquina por su nombre (case‑sensitive). */
    Optional<Machine> findByName(String name);
}