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
import org.springframework.http.HttpMethod;
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
        // Schritt 39m: Passkey/WebAuthn muss auch ohne bestehende JWT-Sitzung und mit Browser-Preflight laufen.
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/api/auth/passkey/**").permitAll()
        .requestMatchers(
          "/api/auth/login", "/api/tts/status", "/api/tts/audio",
          "/api/auth/totp/setup",
          "/api/auth/totp/confirm",
          "/api/system/status",
          "/api/system/startup-check",
          "/api/communication/login-news",
          "/api/public/module-settings",
          "/api/ui-translations",
          "/api/ui-translations/**",
          "/api/invoices/lbd/preview",
          "/api/invoices/*/pdf",
          "/api/invoices/*/pdf-openhtml",
          "/api/invoices/*/zugferd.xml",
          "/api/invoice-portal/**",
          "/actuator/health", "/api/tts/status"
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
        .requestMatchers("/api/communication/**").authenticated()

        .anyRequest().authenticated())
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
      .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    // Schritt 39m: lokale Tests laufen je nach Rechner/Startskript auf 5173, 5174, 8080, localhost oder 127.0.0.1.
    // Feste Origins fuehren bei Passkey-Preflights leicht zu HTTP 403.
    cfg.setAllowedOriginPatterns(List.of(
      "http://localhost:*", "http://127.0.0.1:*",
      "https://localhost:*", "https://127.0.0.1:*",
      "http://192.168.*:*", "http://10.*:*",
      "http://172.16.*:*", "http://172.17.*:*", "http://172.18.*:*", "http://172.19.*:*",
      "http://172.20.*:*", "http://172.21.*:*", "http://172.22.*:*", "http://172.23.*:*",
      "http://172.24.*:*", "http://172.25.*:*", "http://172.26.*:*", "http://172.27.*:*",
      "http://172.28.*:*", "http://172.29.*:*", "http://172.30.*:*", "http://172.31.*:*"
    ));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setExposedHeaders(List.of("Retry-After", "Content-Type"));
    cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
