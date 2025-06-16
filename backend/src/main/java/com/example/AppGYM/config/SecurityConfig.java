// backend/src/main/java/com/example/AppGYM/config/SecurityConfig.java
package com.example.AppGYM.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtFilter jwtFilter;
  private final UserDetailsService users;

  /* ===== Beans ===== */
  @Bean public PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }

  @Bean
  public AuthenticationManager authManager(){
    DaoAuthenticationProvider p = new DaoAuthenticationProvider();
    p.setUserDetailsService(users);
    p.setPasswordEncoder(passwordEncoder());
    return p::authenticate;
  }

  /* ===== Cadena de seguridad ===== */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http
            .csrf(cs -> cs.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            /* ——— Autorizaciones ——— */
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    /* NUEVO: la ruta de reportes sólo necesita estar autenticado */
                    .requestMatchers("/api/report/**").authenticated()

                    .anyRequest().authenticated()
            )

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .cors(Customizer.withDefaults());

    return http.build();
  }
}
