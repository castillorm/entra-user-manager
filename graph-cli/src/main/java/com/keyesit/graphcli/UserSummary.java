package com.keyesit.graphcli;

import java.time.OffsetDateTime;

public record UserSummary(
    String id,
    String displayName,
    String userPrincipalName,
    String mail,
    String userType,
    Boolean accountEnabled,
    String externalUserState,
    OffsetDateTime externalUserStateChangeDateTime) {
}
