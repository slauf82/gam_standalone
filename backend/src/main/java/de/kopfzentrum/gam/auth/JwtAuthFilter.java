package de.kopfzentrum.gam.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import de.kopfzentrum.gam.security.SecurityAuditService;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final AccountRepository accounts;
  private final SecurityAuditService audit;
  public JwtAuthFilter(JwtService jwtService, AccountRepository accounts, SecurityAuditService audit) {
    this.jwtService = jwtService;
    this.accounts = accounts;
    this.audit = audit;
  }

  /**
   * StreamingResponseBody setzt die Verarbeitung als ASYNC-Dispatch fort.
   * OncePerRequestFilter ueberspringt ASYNC standardmaessig; dadurch ging der
   * JWT-SecurityContext nach Beginn des Discovery-Streams verloren und der
   * bereits gestartete Stream endete mit AccessDenied/response committed.
   */
  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    String auth = request.getHeader("Authorization");
    if (auth != null && auth.startsWith("Bearer ")) {
      try {
        String username = jwtService.subject(auth.substring(7));
        accounts.findByUsername(username).ifPresent(a -> {
          AuthenticatedUser user = new AuthenticatedUser(a);
          SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
          );
        });
      } catch (Exception ex) {
        SecurityContextHolder.clearContext();
        audit.tokenRejected(ex.getClass().getSimpleName());
      }
    }
    chain.doFilter(request, response);
  }
}
