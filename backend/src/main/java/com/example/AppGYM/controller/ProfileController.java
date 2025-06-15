package com.example.AppGYM.controller;

import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final UserRepository users;

    @GetMapping
    public User me(@AuthenticationPrincipal User user) {
        return user;
    }

    @PutMapping
    public User update(@AuthenticationPrincipal User user, @RequestBody User body) {
        user.setFirstName(body.getFirstName());
        user.setLastName(body.getLastName());
        user.setAge(body.getAge());
        user.setHeightCm(body.getHeightCm());
        user.setWeightKg(body.getWeightKg());
        user.setMeasurements(body.getMeasurements());
        return users.save(user);
    }
}