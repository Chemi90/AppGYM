package com.example.AppGYM.controller;

import com.example.AppGYM.dto.LoginDto;
import com.example.AppGYM.dto.RegisterDto;
import com.example.AppGYM.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody RegisterDto dto) {
        return Map.of("token", authService.register(dto));
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginDto dto) {
        return Map.of("token", authService.login(dto));
    }
}