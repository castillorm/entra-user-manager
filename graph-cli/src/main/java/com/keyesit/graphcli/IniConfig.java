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

  public static IniConfig load(Path path) throws IOException {
    IniConfig cfg = new IniConfig();
    cfg.parse(path);
    return cfg;
  }

  public String getNormalized(String section, String key, boolean required) {
    String val = get(section, key, required).trim();
    return stripQuotes(val);
  }

  private static String stripQuotes(String s) {
    if ((s.startsWith("\"") && s.endsWith("\"")) ||
        (s.startsWith("'") && s.endsWith("'"))) {
      return s.substring(1, s.length() - 1).trim();
    }
    return s;
  }

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

  public String get(String section, String key, boolean required) {
    String val = getRaw(section, key);

    if ((val == null || val.isBlank()) && required) {
      throw new IllegalArgumentException("Missing required config: " + section + "." + key);
    }

    if (val == null)
      return null;

    // Normalize: trim + strip wrapping quotes to support INI values containing '#'
    return stripQuotes(val.trim());
  }

  private String getRaw(String section, String key) {
    Map<String, String> sec = sections.get(section);
    return (sec == null) ? null : sec.get(key);
  }

  public String getOrDefault(String section, String key, String def) {
    String v = get(section, key, false);
    return (v == null || v.isBlank()) ? def : v;
  }

  public boolean getBoolean(String section, String key, boolean def) {
    String v = get(section, key, false);
    if (v == null)
      return def;
    return v.trim().equalsIgnoreCase("true") || v.trim().equalsIgnoreCase("yes") || v.trim().equals("1");
  }
}
