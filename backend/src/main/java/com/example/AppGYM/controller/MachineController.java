package com.example.AppGYM.controller;

import com.example.AppGYM.model.Machine;
import com.example.AppGYM.model.User;
import com.example.AppGYM.model.UserMachine;
import com.example.AppGYM.repository.MachineRepository;
import com.example.AppGYM.repository.UserMachineRepository;
import lombok.Data;
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
    public List<UserMachine> list(@AuthenticationPrincipal User user) {
        return userMachines.findByUserId(user.getId());
    }

    @PostMapping
    public UserMachine upsert(@AuthenticationPrincipal User user, @RequestBody MachineDto dto) {
        Machine machine = machines.findByName(dto.getName()).orElseGet(() -> {
            Machine m = new Machine();
            m.setName(dto.getName());
            return machines.save(m);
        });
        return userMachines.save(userMachines.findByUserId(user.getId()).stream()
                .filter(um -> um.getMachine().getId().equals(machine.getId()))
                .findFirst()
                .map(um -> { um.setWeightKg(dto.getWeightKg()); return um; })
                .orElseGet(() -> {
                    UserMachine um = new UserMachine();
                    um.setUser(user);
                    um.setMachine(machine);
                    um.setWeightKg(dto.getWeightKg());
                    return um;
                }));
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        userMachines.findById(id).filter(um -> um.getUser().getId().equals(user.getId())).ifPresent(userMachines::delete);
    }

    @Data
    public static class MachineDto {
        private String name;
        private Double weightKg;
    }
}