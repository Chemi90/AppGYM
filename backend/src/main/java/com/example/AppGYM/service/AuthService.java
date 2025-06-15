package com.example.AppGYM.service;

import com.example.AppGYM.dto.LoginDto;
import com.example.AppGYM.dto.RegisterDto;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final JwtService jwt;
    private final PasswordEncoder enc;

    /* ---------- Registro ---------- */
    public String register(RegisterDto dto) {
        if (!dto.getPassword().equals(dto.getConfirm()))
            throw new IllegalArgumentException("Las contraseñas no coinciden");

        if (users.existsByEmail(dto.getEmail()))
            throw new IllegalStateException("El correo ya está registrado");

        User u = new User();
        u.setEmail(dto.getEmail());
        u.setPassword(enc.encode(dto.getPassword()));
        users.save(u);

        return jwt.generateToken(u.getEmail());
    }

    /* ---------- Login ---------- */
    public String login(LoginDto dto) {
        User u = users.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));

        if (!enc.matches(dto.getPassword(), u.getPassword()))
            throw new IllegalArgumentException("Credenciales incorrectas");

        return jwt.generateToken(u.getEmail());
    }
}
