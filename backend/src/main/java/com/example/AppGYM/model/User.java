// backend/src/main/java/com/example/AppGYM/model/User.java
package com.example.AppGYM.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Entity @Data
public class User implements UserDetails {         // ← implementa

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    /* -------- Perfil básico -------- */
    private String firstName, lastName;
    private Integer age;
    private Double heightCm, weightKg;

    /* -------- Medidas (cm) -------- */
    private Double neckCm, chestCm, waistCm, lowerAbsCm, hipCm,
            bicepsCm, bicepsFlexCm, forearmCm, thighCm, calfCm;

    /* -------- Fotos (URLs) -------- */
    private String frontImgUrl, sideImgUrl, backImgUrl;

    /* === Métodos UserDetails (implementación mínima) === */
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();        // sin roles
    }
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
