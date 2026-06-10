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
          "/api/auth/totp/setup",
          "/api/auth/totp/confirm",
          "/api/auth/passkey/status",
          "/api/auth/passkey/register/options",
          "/api/auth/passkey/register/finish",
          "/api/auth/passkey/login/options",
          "/api/auth/passkey/login/finish",
          "/api/system/status",
          "/api/system/startup-check",
          "/api/invoices/lbd/preview",
          "/api/invoices/*/pdf",
          "/api/invoices/*/pdf-debug",
          "/api/invoices/*/zugferd.xml",
          "/api/invoice-portal/**",
          "/actuator/health"
        ).permitAll()
        .requestMatchers("/api/auth/me", "/api/auth/menu").authenticated()

        // Administration: bewusst hart begrenzt. Benutzer/Rollen duerfen nur Admins sehen/aendern.
        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "ADMINISTRATOR", "SUPERADMIN")

        // Fachrechte werden im GAM 2.0 kompatibel ueber userapplication geprueft.
        // SUPERADMIN ist globaler Bypass; alle anderen Rechte sind an APPLICATION/RGESELLSCHAFTS_ID gekoppelt.
        .requestMatchers("/api/invoices/**").authenticated()
        .requestMatchers("/api/inventory-warehouse/**").authenticated()
        .requestMatchers("/api/inventory/**").authenticated()
        .requestMatchers("/api/warehouse/**").authenticated()
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
