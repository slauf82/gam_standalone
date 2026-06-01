package de.kopfzentrum.gam.config;

import de.kopfzentrum.gam.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
    return http.csrf(csrf -> csrf.disable())
      .cors(cors -> {})
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
          "/api/auth/login",
          "/api/system/status",
          "/api/system/startup-check",
          "/api/invoices/lbd/preview",
          "/api/invoices/*/pdf",
          "/api/invoices/*/pdf-debug",
          "/api/invoices/*/zugferd.xml",
          "/actuator/health"
        ).permitAll()
        .requestMatchers("/api/auth/me", "/api/auth/menu").authenticated()

        // Administration: bewusst hart begrenzt. Benutzer/Rollen duerfen nur Admins sehen/aendern.
        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPERADMIN")

        // Rechnungen: im alten GAM fachlich kritisch, daher nur Admin/Rechnung.
        .requestMatchers("/api/invoices/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPERADMIN", "RECHNUNG")

        // Inventar und Lager getrennt, gemeinsame Buchungen fuer beide Rollen erlaubt.
        .requestMatchers("/api/inventory-warehouse/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPERADMIN", "INVENTAR", "LAGER")
        .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPERADMIN", "INVENTAR")
        .requestMatchers("/api/warehouse/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPERADMIN", "LAGER")

        // Gesamt-GAM: Lese-/Rahmenmodule bleiben angemeldet erreichbar; spaetere Feingranularitaet erfolgt im Service.
        .requestMatchers("/api/gam/personnel/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPERADMIN", "PERSONAL")
        .requestMatchers("/api/gam/cashbook/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPERADMIN")
        .requestMatchers("/api/gam/**").authenticated()

        .anyRequest().authenticated())
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
      .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
