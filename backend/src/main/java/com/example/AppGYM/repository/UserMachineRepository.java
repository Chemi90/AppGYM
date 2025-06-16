// backend/src/main/java/com/example/AppGYM/repository/UserMachineRepository.java
package com.example.AppGYM.repository;

import com.example.AppGYM.model.UserMachine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMachineRepository extends JpaRepository<UserMachine,Long> {

    List<UserMachine> findByUserId(Long userId);

    Optional<UserMachine> findByUserIdAndMachineId(Long userId, Long machineId);
}
