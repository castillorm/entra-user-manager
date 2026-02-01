
package com.keyesit.graphcli;

import java.time.OffsetDateTime;

public final class UserSummary {

  private final String id;
  private final String displayName;
  private final String userPrincipalName;
  private final String mail;
  private final String userType;
  private final Boolean accountEnabled;
  private final String externalUserState;
  private final OffsetDateTime externalUserStateChangeDateTime;

  public UserSummary(
      String id,
      String displayName,
      String userPrincipalName,
      String mail,
      String userType,
      Boolean accountEnabled,
      String externalUserState,
      OffsetDateTime externalUserStateChangeDateTime) {
    this.id = id;
    this.displayName = displayName;
    this.userPrincipalName = userPrincipalName;
    this.mail = mail;
    this.userType = userType;
    this.accountEnabled = accountEnabled;
    this.externalUserState = externalUserState;
    this.externalUserStateChangeDateTime = externalUserStateChangeDateTime;
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getUserPrincipalName() {
    return userPrincipalName;
  }

  public String getMail() {
    return mail;
  }

  public String getUserType() {
    return userType;
  }

  public Boolean getAccountEnabled() {
    return accountEnabled;
  }

  public String getExternalUserState() {
    return externalUserState;
  }

  public OffsetDateTime getExternalUserStateChangeDateTime() {
    return externalUserStateChangeDateTime;
  }

  @Override
  public String toString() {
    return "UserSummary{" +
        "id='" + id + '\'' +
        ", displayName='" + displayName + '\'' +
        ", userPrincipalName='" + userPrincipalName + '\'' +
        ", mail='" + mail + '\'' +
        ", userType='" + userType + '\'' +
        ", accountEnabled=" + accountEnabled +
        ", externalUserState='" + externalUserState + '\'' +
        ", externalUserStateChangeDateTime=" + externalUserStateChangeDateTime +
        '}';
  }
}
