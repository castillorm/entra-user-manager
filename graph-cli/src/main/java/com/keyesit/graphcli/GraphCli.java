package com.keyesit.graphcli;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GraphCli {

  private static Logger log;

  public static void main(String[] args) throws Exception {
    try {
      Path projectDir = Path.of("").toAbsolutePath();
      Path configPath = projectDir.resolve("config.ini");
      Path authPath = projectDir.resolve("auth.ini");
      IniConfig auth_ini = IniConfig.load(authPath);
      IniConfig config_ini = IniConfig.load(configPath);
      AppConfig cfg = AppConfig.fromIni(config_ini);
      log = LoggerFactory.getLogger(GraphCli.class);
      Path logDir = projectDir.resolve("log");
      Files.createDirectories(logDir);
      System.setProperty("LOG_DIR", logDir.toString());
      System.setProperty("LOG_FILE", "graphcli.log");
      System.out.println("Starting " + cfg.mode + ".");
      log.info("STARTING - MODE : {}", cfg.mode);
      GraphServiceClient graph = buildGraphClient(auth_ini);

      switch (cfg.mode) {
        case search -> runSearch(cfg, graph);
        case delete -> runDelete(cfg, graph);
        case invite -> runInvite(cfg, config_ini, graph);
        default -> {
          System.out.println("Unsupported mode :{}\n try: 'search', 'delete' or 'invite'" + cfg.mode);
          log.warn("Unsupported mode: {}", cfg.mode);
          System.exit(2);
        }
      }

      System.out.println("Done.");
      log.info("DONE");
      // It was hanging in after completion waiting on something in the backend
      // so I made it explicitly exit after completion
      System.exit(0);
    } catch (ConfigException e) {
      System.err.println("Config error:");
      System.err.println("  " + e.getMessage());
      System.err.println();
      System.err.println("Fix config.ini/auth.ini and try again.");
      System.exit(2);
    }

  }

  private static GraphServiceClient buildGraphClient(IniConfig ini) {
    String tenantId = ini.get("auth", "tenantId");
    String clientId = ini.get("auth", "clientId");
    String clientSecret = ini.get("auth", "clientSecret");
    String scope = ini.get("auth", "scope");
    ClientSecretCredential credential = new ClientSecretCredentialBuilder()
        .tenantId(tenantId)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .build();

    return new GraphServiceClient(credential, new String[] { scope });
  }

  private static void runInvite(AppConfig cfg, IniConfig ini, GraphServiceClient graph) {
    String email = ini.get("invite", "email").trim();
    String redirectUrl = ini.get("invite", "redirectUrl").trim();
    boolean sendMsg = ini.getBoolean("invite", "sendInvitationMessage");
    System.out.println("=== Execution Plan ===");
    System.out.println("Action: CREATE guest invitation");
    System.out.println("  email: " + email);
    System.out.println("  redirectUrl: " + redirectUrl);
    System.out.println("  sendInvitationMessage: " + sendMsg);
    System.out.println("======================");
    GraphGuestInviter inviter = new GraphGuestInviter(graph);
    var created = inviter.invite(email, redirectUrl, sendMsg);
    System.out.println("Invitation created.");
    System.out.println("- invitedUserEmailAddress = "
        + safe(created.getInvitedUserEmailAddress()));

    if (created.getInvitedUser() != null) {
      System.out.println("- invitedUserId = "
          + safe(created.getInvitedUser().getId()));
    } else {
      System.out.println("- invitedUserId = (not returned yet)");
      System.out.println("- invitedUserDisplayName = (not returned yet)");
    }
    System.out.println("======== INVITE =========");
    System.out.println(safe(created.getInviteRedeemUrl()));
    System.out.println("=========================");
  }

  private static void runSearch(AppConfig cfg, GraphServiceClient graph) {
    log.debug("SEARCH query='{}' maxResults={}", cfg.query, cfg.maxResults);
    GraphUserFinder finder = new GraphUserFinder(graph);
    List<UserSummary> users = finder.find(cfg);
    printCandidates(users);
    log.debug("RESULT searchCount={}", users.size());
  }

  private static void runDelete(AppConfig cfg, GraphServiceClient graph) {
    log.debug("DELETE requested query='{}'", cfg.query);
    GraphUserFinder finder = new GraphUserFinder(graph);
    List<UserSummary> matches = finder.find(cfg);

    if (matches.isEmpty()) {
      System.out.println("No user found. No changes made.");
      log.debug("MATCH count=0");
      return;
    }

    if (matches.size() > 1) {
      System.out.println("Multiple users matched; refusing to delete. No changes made.");
      printCandidates(matches);
      log.debug("MATCH count={} refusingToProceed=true", matches.size());
      return;
    }

    UserSummary target = matches.get(0);
    GraphUserDeleter deleter = new GraphUserDeleter(graph);
    try {
      deleter.deleteById(target.id());
      System.out.println("Delete completed.");
      log.warn("RESULT status=SUCCESS deletedUserId={}", safe(target.id()));
    } catch (Exception e) {
      System.out.println("Delete failed. No changes made beyond attempted delete.");
      System.out.println("ERROR: " + e.getMessage());
      log.error("RESULT status=FAILED deletedUserId={} error={}", safe(target.id()), e.toString(), e);
      System.exit(3);
    }
  }

  private static void printCandidates(List<UserSummary> users) {
    System.out.println("========");
    System.out.println("Matches: " + users.size());
    for (UserSummary u : users) {
      System.out.println("========");
      StringBuilder sb = new StringBuilder(safe("id = " + u.id() + "\n"));
      sb.append("name = " + u.displayName() + "\n");
      sb.append("upn = " + u.userPrincipalName() + "\n");
      sb.append("mail = " + u.mail() + "\n");
      sb.append("userType = " + u.userType() + "\n");
      if (u.externalUserState() != null) {
        sb.append("InviteState = " + u.externalUserState() + "\n");
      }
      sb.append("acountEnabled = " + u.accountEnabled());
      ;
      System.out.println(sb);
    }

  }

  private static String safe(String s) {
    return (s == null) ? "" : s;
  }
}
