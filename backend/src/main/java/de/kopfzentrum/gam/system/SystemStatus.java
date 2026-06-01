package de.kopfzentrum.gam.system;

import java.util.List;
import java.util.Map;

public record SystemStatus(
    boolean databaseAvailable,
    long accountCount,
    boolean lbdAvailable,
    String lbdFile,
    List<String> lbdSearchFolders,
    Map<String, Object> modules
) {}
