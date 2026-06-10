package de.kopfzentrum.gam.invoice.lbd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Service
public class LbdService {
  private final List<Path> searchFolders;
  private final String preferredFileName;
  private final Charset charset;

  public LbdService(
    @Value("${app.invoice.lbd.file:}") String file,
    @Value("${app.invoice.lbd.search-folders:./config,./daten/rechnung,.}") String folders,
    @Value("${app.invoice.lbd.charset:windows-1252}") String charsetName
  ) {
    this.preferredFileName = (file == null || file.isBlank()) ? "" : file.trim();
    this.searchFolders = Arrays.stream(folders.split(","))
      .map(String::trim).filter(s -> !s.isBlank()).map(Paths::get).toList();
    this.charset = Charset.forName(charsetName == null || charsetName.isBlank() ? StandardCharsets.UTF_8.name() : charsetName);
  }

  public Optional<Path> findFile(String requestedName) throws IOException {
    if (requestedName != null && !requestedName.isBlank()) {
      Path direct = Paths.get(requestedName.trim());
      if (Files.exists(direct) && Files.isRegularFile(direct)) return Optional.of(direct.toAbsolutePath().normalize());
      for (Path folder : searchFolders) {
        Path candidate = folder.resolve(requestedName.trim());
        if (Files.exists(candidate) && Files.isRegularFile(candidate)) return Optional.of(candidate.toAbsolutePath().normalize());
      }
    }
    if (!preferredFileName.isBlank()) {
      Path configured = Paths.get(preferredFileName);
      if (Files.exists(configured) && Files.isRegularFile(configured)) return Optional.of(configured.toAbsolutePath().normalize());
      for (Path folder : searchFolders) {
        Path candidate = folder.resolve(preferredFileName);
        if (Files.exists(candidate) && Files.isRegularFile(candidate)) return Optional.of(candidate.toAbsolutePath().normalize());
      }

// Demo/first-run fallback: use a neutral demo LBD if no explicit file was requested.
// This improves the GitHub demo experience and avoids a missing-LBD warning
// when config/demo-lbd/max.mustermann.lbd is present.
if (preferredFileName.isBlank()) {
  String demoFileName = "max.mustermann.lbd";
  for (Path folder : searchFolders) {
    Path candidate = folder.resolve(demoFileName);
    if (Files.exists(candidate) && Files.isRegularFile(candidate)) return Optional.of(candidate.toAbsolutePath().normalize());
  }
}

    }
    for (Path folder : searchFolders) {
      if (!Files.isDirectory(folder)) continue;
      try (Stream<Path> s = Files.list(folder)) {
        Optional<Path> first = s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".lbd"))
          .sorted().findFirst();
        if (first.isPresent()) return Optional.of(first.get().toAbsolutePath().normalize());
      }
    }
    return Optional.empty();
  }

  public Optional<Path> findFirstLbdFile() {
    try {
      return findFile(null);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public List<String> searchFolders() {
    return searchFolders.stream().map(Path::toString).toList();
  }

  public LbdRecipient preview(String requestedName) throws IOException {
    Optional<Path> found = findFile(requestedName);
    if (found.isEmpty()) {
      return new LbdRecipient(false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, Map.of());
    }
    return parse(found.get());
  }

  public LbdRecipient parse(Path file) throws IOException {
    Map<String, String> raw = new LinkedHashMap<>();
    String patientNumber = null, nameSuffix = null, lastName = null, firstName = null, birthDate = null, title = null;
    String insuranceNumber = null, postalCode = null, city = null, country = null, street = null, insuranceType = null, salutation = null;
    Integer salutationIndex = null;

    try (BufferedReader br = Files.newBufferedReader(file, charset)) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.length() < 7) continue;
        String code = line.substring(3, 7);
        String value = line.length() > 7 ? line.substring(7).trim() : "";
        raw.put(code, value);
        switch (code) {
          case "3000" -> patientNumber = value;
          case "3100" -> nameSuffix = value;
          case "3101" -> lastName = value;
          case "3102" -> firstName = value;
          case "3103" -> birthDate = value;
          case "3104" -> title = value;
          case "3105" -> insuranceNumber = value;
          case "3106" -> {
            int split = value.indexOf(' ');
            if (split > 0) { postalCode = value.substring(0, split).trim(); city = value.substring(split + 1).trim(); }
            else { country = value; }
          }
          case "3107" -> street = value;
          case "3108" -> insuranceType = switch (value) { case "1" -> "selbstversichert"; case "3" -> "familienversichert"; case "5" -> "rentnerversichert"; default -> value; };
          case "3110" -> {
            salutationIndex = parseInt(value);
            salutation = switch (value) { case "1" -> "Herr"; case "2" -> "Frau"; case "3" -> "Divers"; default -> value; };
          }
          default -> { }
        }
      }
    }
    return new LbdRecipient(true, file.toAbsolutePath().normalize().toString(), patientNumber, nameSuffix, lastName, firstName, birthDate, title,
      insuranceNumber, postalCode, city, country, street, insuranceType, salutationIndex, salutation, raw);
  }

  private static Integer parseInt(String value) {
    try { return value == null || value.isBlank() ? null : Integer.parseInt(value.trim()); } catch (Exception e) { return null; }
  }
}
