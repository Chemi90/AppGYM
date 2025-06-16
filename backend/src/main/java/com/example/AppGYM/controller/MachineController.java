// backend/src/main/java/com/example/AppGYM/controller/MachineController.java
package com.example.AppGYM.controller;

import com.example.AppGYM.dto.MachineEntryDto;
import com.example.AppGYM.model.Machine;
import com.example.AppGYM.model.User;
import com.example.AppGYM.model.UserMachine;
import com.example.AppGYM.repository.MachineRepository;
import com.example.AppGYM.repository.UserMachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineRepository machines;
    private final UserMachineRepository userMachines;

    @GetMapping
    public List<UserMachine> list(@AuthenticationPrincipal User u) {
        return userMachines.findByUserId(u.getId());
    }

    @PostMapping
    public void upsert(@AuthenticationPrincipal User u,
                       @RequestBody MachineEntryDto dto) {

        /* -------- obtener o crear la máquina -------- */
        Machine m = machines.findByName(dto.getName())
                .orElseGet(() -> {
                    Machine x = new Machine();      // <- constructor vacío
                    x.setName(dto.getName());
                    return machines.save(x);
                });

        /* -------- obtener o crear vínculo usuario-máquina -------- */
        UserMachine um = userMachines.findByUserIdAndMachineId(u.getId(), m.getId())
                .orElseGet(() -> {
                    UserMachine x = new UserMachine();
                    x.setUser(u);
                    x.setMachine(m);
                    return x;
                });

        /* -------- actualizar valores -------- */
        um.setWeightKg(dto.getWeightKg());
        um.setReps(dto.getReps());
        um.setSets(dto.getSets());
        userMachines.save(um);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal User u, @PathVariable Long id) {
        userMachines.findById(id)
                .filter(um -> um.getUser().getId().equals(u.getId()))
                .ifPresent(userMachines::delete);
    }
}
