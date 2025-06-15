// File: backend/src/main/java/com/example/AppGYM/config/SecurityConfig.java
package com.example.AppGYM.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtFilter jwtFilter;
  private final UserDetailsService users;
  private final PasswordEncoder passwordEnc;
  private final CorsConfig corsConfig;                 // Bean con CorsConfigurationSource

  /* ---------- AuthenticationManager ---------- */
  @Bean
  public AuthenticationManager authManager() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(users);
    provider.setPasswordEncoder(passwordEnc);
    return provider::authenticate;
  }

  /* ---------- Main security chain ---------- */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
            // CORS (estilo Spring Security 6.1+)
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
            // CSRF off porque es API stateless
            .csrf(cs -> cs.disable())
            // JWT = stateless
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Rutas públicas
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated())
            // Filtro JWT antes del UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
