package de.kopfzentrum.gam.auth;

import org.springframework.stereotype.Service;

@Service
public class GamPermissionService {
  private final UserApplicationRepository userApplications;
  public GamPermissionService(UserApplicationRepository userApplications) { this.userApplications = userApplications; }

  public boolean isSuperAdmin(Account account) {
    if (account == null) return false;
    String r = account.normalizedRole();
    return "superadmin".equals(r) || "super-administrator".equals(r);
  }

  public boolean canOpen(Account account, String application, Integer companyId) {
    if (isSuperAdmin(account)) return true;
    return account != null && userApplications.hasApplication(account.username(), application, companyId);
  }

  public boolean canReport(Account account, String application, Integer companyId) {
    if (isSuperAdmin(account)) return true;
    return account != null && userApplications.canReport(account.username(), application, companyId);
  }

  public boolean canWrite(Account account, String application, Integer companyId) {
    if (isSuperAdmin(account)) return true;
    return account != null && userApplications.canWrite(account.username(), application, companyId);
  }
}
