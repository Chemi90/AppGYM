package com.example.AppGYM.repository;

import com.example.AppGYM.model.UserMachine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMachineRepository extends JpaRepository<UserMachine, Long> {
    List<UserMachine> findByUserId(Long userId);
}