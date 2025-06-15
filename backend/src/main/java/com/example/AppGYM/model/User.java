package com.example.AppGYM.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
public class User implements org.springframework.security.core.userdetails.UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private Integer age;
    private Double heightCm;
    private Double weightKg;

    @Column(columnDefinition = "json")
    private String measurements;

    /* ---- UserDetails boilerplate ---- */
    @Override public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() { return java.util.List.of(); }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}