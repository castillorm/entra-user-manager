
package com.keyesit.graphcli;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class IniConfig {

  private final Map<String, Map<String, String>> sections = new HashMap<>();
  private final Path source;

  private IniConfig(Path source) {
    this.source = source;
  }

  // ---------- load ----------

  public static IniConfig load(Path path) {
    if (path == null)
      die("INI path was null");
    if (!Files.exists(path))
      die("Missing config file: " + path.toAbsolutePath());
    if (!Files.isRegularFile(path))
      die("Not a file: " + path.toAbsolutePath());

    IniConfig cfg = new IniConfig(path);
    try {
      cfg.parse(path);
    } catch (IOException e) {
      die("Failed to read config file: " + path.toAbsolutePath() + " (" + e.getMessage() + ")");
    }
    return cfg;
  }

  // ---------- accessors ----------

  /** Returns value or exits with a friendly error if missing/blank. */

  public String get(String section, String key) {
    String v = getRaw(section, key);
    if (v == null) {
      die("Missing required config: " + section + "." + key +
          "\n  file: " + source.toAbsolutePath());
    }
    v = stripQuotes(v.trim());
    if (v.trim().isEmpty()) { // Java 8 replacement for isBlank()
      die("Config value must not be blank: " + section + "." + key +
          "\n  file: " + source.toAbsolutePath());
    }
    return v;
  }

  public boolean getBoolean(String section, String key) {
    String v = get(section, key); // required
    String t = v.trim();
    return t.equalsIgnoreCase("true") || t.equalsIgnoreCase("yes") || t.equals("1");
  }

  // ---------- parsing ----------

  private void parse(Path path) throws IOException {
    String currentSection = "";
    sections.putIfAbsent(currentSection, new HashMap<>());

    try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#") || line.startsWith(";"))
          continue;

        if (line.startsWith("[") && line.endsWith("]")) {
          currentSection = line.substring(1, line.length() - 1).trim();
          sections.putIfAbsent(currentSection, new HashMap<>());
          continue;
        }

        int eq = line.indexOf('=');
        if (eq < 0)
          continue;

        String key = line.substring(0, eq).trim();
        String val = line.substring(eq + 1).trim();
        sections.get(currentSection).put(key, val);
      }
    }
  }

  private String getRaw(String section, String key) {
    Map<String, String> sec = sections.get(section);
    return (sec == null) ? null : sec.get(key);
  }

  private static String stripQuotes(String s) {
    if ((s.startsWith("\"") && s.endsWith("\"")) ||
        (s.startsWith("'") && s.endsWith("'"))) {
      return s.substring(1, s.length() - 1).trim();
    }
    return s;
  }

  // ---------- exit ----------

  private static void die(String message) {
    throw new ConfigException(message);
  }
}
