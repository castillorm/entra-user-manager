package com.keyesit.graphcli;

import java.net.URI;

public final class AppConfig {

  public enum Mode {
    search, delete, invite
  }

  public final Mode mode;
  public final String query;
  public final int maxResults;
  public final URI inviteRedirectUrl;
  public final boolean sendInvitationMessage;

  private AppConfig(
      Mode mode,
      String query,
      int maxResults,
      URI inviteRedirectUrl,
      boolean sendInvitationMessage) {
    this.mode = mode;
    this.query = query;
    this.maxResults = maxResults;
    this.inviteRedirectUrl = inviteRedirectUrl;
    this.sendInvitationMessage = sendInvitationMessage;
  }

  public static AppConfig fromIni(IniConfig cfg) {
    // mode (default search)
    Mode mode = parseMode(cfg.get("operation", "mode"));

    // maxResults (default 25; clamp 1..100)
    int maxResults = parseIntInRange(
        cfg.get("search", "maxResults"),
        1, 100, "search.maxResults");

    // query is conditionally required
    String query = "";
    if (mode == Mode.search || mode == Mode.delete) {
      query = cfg.get("search", "query"); // exits if missing/blank (based on your IniConfig.get())
    } else {
      query = cfg.get("search", "query").trim();
    }

    // invite fields are only required for invite mode
    URI redirectUrl = null;
    boolean sendInvitationMessage = cfg.getBoolean("invite", "sendInvitationMessage");
    if (mode == Mode.invite) {
      redirectUrl = parseAbsoluteUri(cfg.get("invite", "redirectUrl"), "invite.redirectUrl");
    }

    return new AppConfig(mode, query, maxResults, redirectUrl, sendInvitationMessage);
  }

  private static Mode parseMode(String raw) {
    String v = raw == null ? "" : raw.trim();
    try {
      return Mode.valueOf(v);
    } catch (Exception e) {
      die("operation.mode must be one of: search, delete, invite (got: " + raw + ")");
      return Mode.search; // unreachable
    }
  }

  private static int parseIntInRange(String raw, int min, int max, String field) {
    try {
      int v = Integer.parseInt(raw.trim());
      if (v < min || v > max)
        die(field + " must be between " + min + " and " + max + " (got: " + v + ")");
      return v;
    } catch (Exception e) {
      die(field + " must be an integer (got: " + raw + ")");
      return min; // unreachable
    }
  }

  private static URI parseAbsoluteUri(String raw, String field) {
    try {
      URI u = URI.create(raw.trim());
      if (u.getScheme() == null || u.getHost() == null) {
        die(field + " must be an absolute URL (got: " + raw + ")");
      }
      return u;
    } catch (Exception e) {
      die(field + " must be a valid URL (got: " + raw + ")");
      return null; // unreachable
    }
  }

  private static void die(String message) {
    throw new ConfigException(message);
  }
}
