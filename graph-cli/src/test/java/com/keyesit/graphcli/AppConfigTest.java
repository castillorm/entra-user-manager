package com.keyesit.graphcli;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void validMinimalConvertToGuestConfig_parsesSuccessfully() throws Exception {
        String ini = """
            [operation]
            mode=convertToGuest
            dryRun=true

            [search]
            subMode=auto
            query=alice@contoso.com
            maxResults=25

            [invite]
            redirectUrl=https://myapps.microsoft.com
            sendInvitationMessage=true

            [logging]
            verbose=false
            dir=log
            """;

        Path tmp = Files.createTempFile("config", ".ini");
        Files.writeString(tmp, ini);

        IniConfig cfg = IniConfig.load(tmp);
        AppConfig app = AppConfig.fromIni(cfg);

        assertEquals(AppConfig.Mode.convertToGuest, app.mode);
        assertTrue(app.dryRun);
        assertEquals(AppConfig.SearchSubMode.auto, app.subMode);
        assertEquals("alice@contoso.com", app.query);
        assertEquals(25, app.maxResults);
        assertEquals("https://myapps.microsoft.com", app.inviteRedirectUrl.toString());
        assertFalse(app.verbose);
        assertEquals("log", app.logDir);
    }

    @Test
    void missingSearchQuery_failsValidation() throws Exception {
        String ini = """
            [operation]
            mode=search

            [search]
            subMode=auto
            """;

        Path tmp = Files.createTempFile("config", ".ini");
        Files.writeString(tmp, ini);

        IniConfig cfg = IniConfig.load(tmp);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AppConfig.fromIni(cfg)
        );

        assertTrue(ex.getMessage().contains("search.query"));
    }
}

