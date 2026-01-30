package com.keyesit.graphcli;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

  // ---------- happy paths ----------

  @Test
  void searchMode_parsesSuccessfully() throws Exception {
    String ini = """
        [operation]
        mode=search

        [search]
        query=alice@contoso.com
        maxResults=25

        [invite]
        redirectUrl=
        sendInvitationMessage=true
        """;

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
    String ini = """
        [operation]
        mode=invite

        [search]
        query=ignored
        maxResults=10

        [invite]
        redirectUrl=https://myapps.microsoft.com
        sendInvitationMessage=false
        """;

    AppConfig cfg = loadAppConfig(ini);

    assertEquals(AppConfig.Mode.invite, cfg.mode);
    assertEquals("https://myapps.microsoft.com", cfg.inviteRedirectUrl.toString());
    assertFalse(cfg.sendInvitationMessage);
  }

  // ---------- failure cases ----------

  @Test
  void missing_search_query_throws() {
    String ini = """
        [operation]
        mode=search

        [search]
        maxResults=25

        [invite]
        redirectUrl=
        sendInvitationMessage=true
        """;

    ConfigException ex = assertThrows(ConfigException.class, () -> loadAppConfig(ini));
    assertTrue(ex.getMessage().contains("search.query"));
  }

  @Test
  void missing_search_maxResults_throws() {
    String ini = """
        [operation]
        mode=search

        [search]
        query=alice@contoso.com

        [invite]
        redirectUrl=
        sendInvitationMessage=true
        """;

    ConfigException ex = assertThrows(ConfigException.class, () -> loadAppConfig(ini));
    assertTrue(ex.getMessage().contains("search.maxResults"));
  }

  @Test
  void inviteMode_missing_redirectUrl_throws() {
    String ini = """
        [operation]
        mode=invite

        [search]
        query=alice
        maxResults=25

        [invite]
        redirectUrl=
        sendInvitationMessage=true
        """;

    ConfigException ex = assertThrows(ConfigException.class, () -> loadAppConfig(ini));
    assertTrue(ex.getMessage().contains("invite.redirectUrl"));
  }

  @Test
  void invalid_mode_throws() {
    String ini = """
        [operation]
        mode=bogus

        [search]
        query=alice
        maxResults=25

        [invite]
        redirectUrl=
        sendInvitationMessage=true
        """;

    ConfigException ex = assertThrows(ConfigException.class, () -> loadAppConfig(ini));
    assertTrue(ex.getMessage().contains("operation.mode"));
  }

  // ---------- helpers ----------

  private static AppConfig loadAppConfig(String iniText) throws Exception {
    Path tmp = Files.createTempFile("config", ".ini");
    Files.writeString(tmp, iniText);
    IniConfig cfg = IniConfig.load(tmp);
    return AppConfig.fromIni(cfg);
  }
}
