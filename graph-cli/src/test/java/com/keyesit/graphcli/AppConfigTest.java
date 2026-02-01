
package com.keyesit.graphcli;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

  // ---------- happy paths ----------

  @Test
  void searchMode_parsesSuccessfully() throws Exception {
    String ini = "[operation]\n" +
        "mode=search\n" +
        "\n" +
        "[search]\n" +
        "query=alice@contoso.com\n" +
        "maxResults=25\n" +
        "\n" +
        "[invite]\n" +
        "redirectUrl=\n" +
        "sendInvitationMessage=true\n";

    AppConfig cfg = loadAppConfig(ini);

    assertEquals(AppConfig.Mode.search, cfg.mode);
    assertEquals("alice@contoso.com", cfg.query);
    assertEquals(25, cfg.maxResults);

    // Not used in search mode — null is expected
    assertNull(cfg.inviteRedirectUrl);
    assertTrue(cfg.sendInvitationMessage);
  }

  @Test
  void inviteMode_parsesSuccessfully() throws Exception {
    String ini = "[operation]\n" +
        "mode=invite\n" +
        "\n" +
        "[search]\n" +
        "query=ignored\n" +
        "maxResults=10\n" +
        "\n" +
        "[invite]\n" +
        "redirectUrl=https://myapps.microsoft.com\n" +
        "sendInvitationMessage=false\n";

    AppConfig cfg = loadAppConfig(ini);

    assertEquals(AppConfig.Mode.invite, cfg.mode);
    assertEquals("https://myapps.microsoft.com", cfg.inviteRedirectUrl.toString());
    assertFalse(cfg.sendInvitationMessage);
  }

  // ---------- failure cases (implementation throws ConfigException) ----------

  @Test
  void missing_search_query_throws() {
    String ini = "[operation]\n" +
        "mode=search\n" +
        "\n" +
        "[search]\n" +
        "maxResults=25\n" +
        "\n" +
        "[invite]\n" +
        "redirectUrl=\n" +
        "sendInvitationMessage=true\n";

    ConfigException ex = assertThrows(ConfigException.class, () -> loadAppConfig(ini));
    assertTrue(ex.getMessage().contains("search.query"));
  }

  @Test
  void missing_search_maxResults_throws() {
    String ini = "[operation]\n" +
        "mode=search\n" +
        "\n" +
        "[search]\n" +
        "query=alice@contoso.com\n" +
        "\n" +
        "[invite]\n" +
        "redirectUrl=\n" +
        "sendInvitationMessage=true\n";

    ConfigException ex = assertThrows(ConfigException.class, () -> loadAppConfig(ini));
    assertTrue(ex.getMessage().contains("search.maxResults"));
  }

  @Test
  void inviteMode_missing_redirectUrl_throws() {
    String ini = "[operation]\n" +
        "mode=invite\n" +
        "\n" +
        "[search]\n" +
        "query=alice\n" +
        "maxResults=25\n" +
        "\n" +
        "[invite]\n" +
        "redirectUrl=\n" +
        "sendInvitationMessage=true\n";

    ConfigException ex = assertThrows(ConfigException.class, () -> loadAppConfig(ini));
    assertTrue(ex.getMessage().contains("invite.redirectUrl"));
  }

  @Test
  void invalid_mode_throws() {
    String ini = "[operation]\n" +
        "mode=bogus\n" +
        "\n" +
        "[search]\n" +
        "query=alice\n" +
        "maxResults=25\n" +
        "\n" +
        "[invite]\n" +
        "redirectUrl=\n" +
        "sendInvitationMessage=true\n";

    ConfigException ex = assertThrows(ConfigException.class, () -> loadAppConfig(ini));
    assertTrue(ex.getMessage().contains("operation.mode"));
  }

  // ---------- helpers ----------

  private static AppConfig loadAppConfig(String iniText) throws Exception {
    Path tmp = Files.createTempFile("config", ".ini");
    Files.write(tmp, iniText.getBytes(StandardCharsets.UTF_8));
    IniConfig cfg = IniConfig.load(tmp);
    return AppConfig.fromIni(cfg);
  }
}
