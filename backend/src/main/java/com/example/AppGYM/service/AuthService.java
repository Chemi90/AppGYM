package com.example.AppGYM.service;

import com.example.AppGYM.dto.LoginDto;
import com.example.AppGYM.dto.RegisterDto;
import com.example.AppGYM.dto.RecaptchaResponse;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RestTemplate rest = new RestTemplate();

    @Value("${recaptcha.secret}")
    private String recaptchaSecret;

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private boolean verifyRecaptcha(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", recaptchaSecret);
        params.add("response", token);
        RecaptchaResponse res = rest.postForObject(VERIFY_URL, new HttpEntity<>(params, headers), RecaptchaResponse.class);
        return res != null && res.isSuccess();
    }

    @Transactional
    public String register(RegisterDto dto) {
        if (!dto.getPassword().equals(dto.getConfirm())) throw new RuntimeException("Las contraseñas no coinciden");
        if (!verifyRecaptcha(dto.getRecaptcha())) throw new RuntimeException("Captcha inválido");
        if (users.existsByEmail(dto.getEmail())) throw new RuntimeException("Correo ya registrado");

        User u = new User();
        u.setEmail(dto.getEmail());
        u.setPassword(encoder.encode(dto.getPassword()));
        users.save(u);
        return jwt.generateToken(u.getEmail());
    }

    public String login(LoginDto dto) {
        User u = users.findByEmail(dto.getEmail()).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!encoder.matches(dto.getPassword(), u.getPassword())) throw new RuntimeException("Credenciales inválidas");
        if (!verifyRecaptcha(dto.getRecaptcha())) throw new RuntimeException("Captcha inválido");
        return jwt.generateToken(u.getEmail());
    }
}