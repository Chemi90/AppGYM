package com.example.AppGYM.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS global:
 *  – Permite el dominio principal de Netlify **y cualquier sub-dominio preview**
 *  – Habilita credenciales (Authorization: Bearer …)
 *  – Acepta cabeceras y métodos típicos
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        /* === dominios permitidos === */
        cfg.setAllowedOriginPatterns(List.of(
                "https://appgymregistro.netlify.app",
                "https://*.netlify.app"            // previews / branches
        ));

        /* === métodos y cabeceras === */
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));          // Content-Type, Authorization, …
        cfg.setAllowCredentials(true);

        /* === aplica a todos los endpoints === */
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
