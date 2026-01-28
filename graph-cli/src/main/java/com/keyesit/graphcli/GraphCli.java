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
    Path projectDir = Path.of("").toAbsolutePath();
    Path configPath = projectDir.resolve("config.ini");
    Path authPath = projectDir.resolve("auth.ini");
    IniConfig auth_ini = IniConfig.load(authPath);
    IniConfig config_ini = IniConfig.load(configPath);
    AppConfig cfg = AppConfig.fromIni(config_ini);
    log = LoggerFactory.getLogger(GraphCli.class);
    Path logDir = projectDir.resolve(cfg.logDir);
    Files.createDirectories(logDir);
    System.setProperty("LOG_DIR", logDir.toString());
    System.setProperty("LOG_FILE", "graphcli.log");
    log.info("STARTING - MODE : {}", cfg.mode);

    log.debug("START mode={} dryRun={} verbose={} config={}",
        cfg.mode, cfg.dryRun, cfg.verbose, configPath.toAbsolutePath());

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

    log.info("DONE");
    // It was haning in after completion waiting on something in the backend
    // so I made it explicitly exit after completion
    System.exit(0);
  }

  private static GraphServiceClient buildGraphClient(IniConfig ini) {
    String tenantId = ini.get("auth", "tenantId", true);
    String clientId = ini.get("auth", "clientId", true);
    String clientSecret = ini.get("auth", "clientSecret", true);
    String scope = ini.getOrDefault("auth", "scope", "https://graph.microsoft.com/.default");

    ClientSecretCredential credential = new ClientSecretCredentialBuilder()
        .tenantId(tenantId)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .build();

    return new GraphServiceClient(credential, new String[] { scope });
  }

  private static void runInvite(AppConfig cfg, IniConfig ini, GraphServiceClient graph) {
    String email = ini.get("invite", "email", true).trim();
    String redirectUrl = ini.get("invite", "redirectUrl", true).trim();
    boolean sendMsg = ini.getBoolean("invite", "sendInvitationMessage", true);

    System.out.println("=== Execution Plan ===");
    System.out.println("Action: CREATE guest invitation");
    System.out.println("  email: " + email);
    System.out.println("  redirectUrl: " + redirectUrl);
    System.out.println("  sendInvitationMessage: " + sendMsg);
    System.out.println("dryRun=" + cfg.dryRun);
    System.out.println("======================");

    if (cfg.dryRun) {
      System.out.println("DRY RUN: no changes made.");
      return;
    }

    GraphGuestInviter inviter = new GraphGuestInviter(graph);
    var created = inviter.invite(email, redirectUrl, sendMsg);

    System.out.println("Invitation created.");
    System.out.println("- invitedUserEmailAddress=" + safe(created.getInvitedUserEmailAddress()));
    System.out
        .println("- invitedUserId=" + (created.getInvitedUser() != null ? safe(created.getInvitedUser().getId()) : ""));
    System.out.println("- inviteRedeemUrl=" + safe(created.getInviteRedeemUrl()));
    System.out.println("=========INVITE==========");
    System.out.println(safe(created.getInviteRedeemUrl()));
    System.out.println("=========================");
  }

  private static void runSearch(AppConfig cfg, GraphServiceClient graph) {
    log.debug("SEARCH subMode={} query='{}' maxResults={}", cfg.subMode, cfg.query, cfg.maxResults);

    GraphUserFinder finder = new GraphUserFinder(graph);
    List<UserSummary> users = finder.find(cfg);

    printCandidates(users);

    log.debug("RESULT searchCount={}", users.size());
  }

  private static void runDelete(AppConfig cfg, GraphServiceClient graph) {
    log.debug("DELETE requested dryRun={} subMode={} query='{}'", cfg.dryRun, cfg.subMode, cfg.query);

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
      System.out.println("Tip: re-run with [search].subMode=id and [search].query=<id>.");
      log.warn("MATCH count={} refusingToProceed=true", matches.size());
      return;
    }

    UserSummary target = matches.get(0);
    if (cfg.verbose) {
      System.out.println("=== Execution Plan ===");
      System.out.printf("Resolved user:%n");
      System.out.printf("  id: %s%n", safe(target.id()));
      System.out.printf("  displayName: %s%n", safe(target.displayName()));
      System.out.printf("  userPrincipalName: %s%n", safe(target.userPrincipalName()));
      System.out.printf("  mail: %s%n", safe(target.mail()));
      System.out.printf("  userType: %s%n", safe(target.userType()));
      System.out.printf("Actions:%n");
      System.out.printf("  1) DELETE user (by id)%n");
      System.out.printf("dryRun=%s%n", cfg.dryRun);
      System.out.println("======================");

      log.warn("PLAN deleteUserId={} upn={} mail={}",
          safe(target.id()), safe(target.userPrincipalName()), safe(target.mail()));

      if (cfg.dryRun) {
        System.out.println("DRY RUN: no changes made.");
        log.warn("RESULT status=DRY_RUN");
        return;
      }
    }
    // Execute
    GraphUserDeleter deleter = new GraphUserDeleter(graph);
    try {
      deleter.deleteById(target.id());
      System.out.println("Delete completed.");
      log.warn("RESULT status=SUCCESS deletedUserId={}", safe(target.id()));
    } catch (Exception e) {
      System.out.println("Delete failed. No changes made beyond attempted delete.");
      System.out.println("Error: " + e.getMessage());
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
        sb.append("InviteState= " + u.externalUserState() + "\n");
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
