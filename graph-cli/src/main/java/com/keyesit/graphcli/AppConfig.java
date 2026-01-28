package com.keyesit.graphcli;

import java.net.URI;
import java.util.Set;

public final class AppConfig {

  public enum Mode {
    search, convertToGuest, delete, invite
  }

  public enum SearchSubMode {
    auto, upn, email, namePrefix, id
  }

  public final Mode mode;
  public final boolean dryRun;

  public final SearchSubMode subMode;
  public final String query;
  public final int maxResults;

  public final URI inviteRedirectUrl;
  public final boolean sendInvitationMessage;

  public final boolean verbose;
  public final String logDir;

  private AppConfig(
      Mode mode,
      boolean dryRun,
      SearchSubMode subMode,
      String query,
      int maxResults,
      URI inviteRedirectUrl,
      boolean sendInvitationMessage,
      boolean verbose,
      String logDir) {
    this.mode = mode;
    this.dryRun = dryRun;
    this.subMode = subMode;
    this.query = query;
    this.maxResults = maxResults;
    this.inviteRedirectUrl = inviteRedirectUrl;
    this.sendInvitationMessage = sendInvitationMessage;
    this.verbose = verbose;
    this.logDir = logDir;
  }

  public static AppConfig fromIni(IniConfig cfg) {
    // ----- operation -----
    String modeRaw = cfg.getOrDefault("operation", "mode", "search").trim();
    Mode mode = parseEnum(modeRaw, Mode.class, "operation.mode");

    boolean dryRun = cfg.getBoolean("operation", "dryRun", true);

    // ----- search -----
    String subModeRaw = cfg.getOrDefault("search", "subMode", "auto").trim();
    SearchSubMode subMode = parseEnum(subModeRaw, SearchSubMode.class, "search.subMode");

    String query = cfg.get("search", "query", true).trim();

    int maxResults = parseInt(cfg.getOrDefault("search", "maxResults", "25"), 1, 100, "search.maxResults");

    // ----- invite -----
    // Invite fields are required for modes that will invite (invite,
    // convertToGuest).
    URI redirectUrl = null;
    boolean sendInvitationMessage = cfg.getBoolean("invite", "sendInvitationMessage", true);

    if (mode == Mode.invite || mode == Mode.convertToGuest) {
      String redirectRaw = cfg.get("invite", "redirectUrl", true).trim();
      redirectUrl = parseUri(redirectRaw, "invite.redirectUrl");
    }

    // ----- logging -----
    boolean verbose = cfg.getBoolean("logging", "verbose", false);
    String logDir = cfg.getOrDefault("logging", "dir", "log").trim();

    validateCrossFieldRules(mode, subMode, query);

    return new AppConfig(
        mode,
        dryRun,
        subMode,
        query,
        maxResults,
        redirectUrl,
        sendInvitationMessage,
        verbose,
        logDir);
  }

  private static void validateCrossFieldRules(Mode mode, SearchSubMode subMode, String query) {
    if (query.isBlank()) {
      throw new IllegalArgumentException("search.query must not be blank");
    }

    // Example UX constraint: destructive modes must never use namePrefix unless
    // result is later resolved to a unique id.
    // For now we just warn via validation rule suggestion; we can enforce later.
    if ((mode == Mode.delete || mode == Mode.convertToGuest) && subMode == SearchSubMode.namePrefix) {
      // allowed, but dangerous; later we'll enforce uniqueness before action.
    }

    // If subMode is email or upn, query should contain '@' (soft validation).
    Set<SearchSubMode> expectsAt = Set.of(SearchSubMode.upn, SearchSubMode.email);
    if (expectsAt.contains(subMode) && !query.contains("@")) {
      throw new IllegalArgumentException("search.query must contain '@' when search.subMode is " + subMode);
    }
  }

  private static <E extends Enum<E>> E parseEnum(String raw, Class<E> enumType, String fieldName) {
    try {
      return Enum.valueOf(enumType, raw.trim());
    } catch (Exception e) {
      StringBuilder allowed = new StringBuilder();
      for (E v : enumType.getEnumConstants()) {
        if (!allowed.isEmpty())
          allowed.append(", ");
        allowed.append(v.name());
      }
      throw new IllegalArgumentException(fieldName + " must be one of: " + allowed + " (got: " + raw + ")");
    }
  }

  private static int parseInt(String raw, int min, int max, String fieldName) {
    try {
      int v = Integer.parseInt(raw.trim());
      if (v < min || v > max) {
        throw new IllegalArgumentException(fieldName + " must be between " + min + " and " + max + " (got: " + v + ")");
      }
      return v;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(fieldName + " must be an integer (got: " + raw + ")");
    }
  }

  private static URI parseUri(String raw, String fieldName) {
    try {
      URI u = URI.create(raw.trim());
      if (u.getScheme() == null || u.getHost() == null) {
        throw new IllegalArgumentException(fieldName + " must be an absolute URL (got: " + raw + ")");
      }
      return u;
    } catch (Exception e) {
      throw new IllegalArgumentException(fieldName + " must be a valid URL (got: " + raw + ")");
    }
  }

  public String prettySummary() {
    return """
        === Effective Configuration ===
        operation.mode=%s
        operation.dryRun=%s

        search.subMode=%s
        search.query=%s
        search.maxResults=%d

        invite.redirectUrl=%s
        invite.sendInvitationMessage=%s
        logging.verbose=%s
        ===============================
        """.formatted(
        mode, dryRun,
        subMode, query, maxResults,
        (inviteRedirectUrl == null ? "(n/a)" : inviteRedirectUrl.toString()),
        sendInvitationMessage,
        verbose, logDir);
  }
}
