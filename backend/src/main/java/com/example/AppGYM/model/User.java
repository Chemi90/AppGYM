// backend/src/main/java/com/example/AppGYM/model/User.java
package com.example.AppGYM.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")               //  ←  ahora el nombre de tabla coincide con la BD
@Data
public class User implements UserDetails {

    /* ────────── columnas ────────── */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    /* perfil y medidas */
    private String  firstName, lastName;
    private Integer age;
    private Double  heightCm, weightKg;

    private Double neckCm, chestCm, waistCm, lowerAbsCm, hipCm,
            bicepsCm, bicepsFlexCm, forearmCm, thighCm, calfCm;

    private String frontImgUrl, sideImgUrl, backImgUrl;

    /* ────────── UserDetails ────────── */
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> "ROLE_USER");
    }
    @Override public String  getUsername()             { return email;    }
    @Override public String  getPassword()             { return password; }
    @Override public boolean isAccountNonExpired()     { return true;     }
    @Override public boolean isAccountNonLocked()      { return true;     }
    @Override public boolean isCredentialsNonExpired() { return true;     }
    @Override public boolean isEnabled()               { return true;     }
}
