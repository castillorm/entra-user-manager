package com.keyesit.graphcli;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;

import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class GraphCli {

    private static final Logger log = LoggerFactory.getLogger(GraphCli.class);

    public static void main(String[] args) throws Exception {
        Path projectDir = Path.of("").toAbsolutePath();
        Path logDir = projectDir.resolve("log");
        Files.createDirectories(logDir);

        // Tell logback where to write logs (./log/graphcli.log by default)
        System.setProperty("LOG_DIR", logDir.toString());
        System.setProperty("LOG_FILE", "graphcli.log");

        Path configPath = (args.length > 0) ? Path.of(args[0]) : projectDir.resolve("config.ini");
        IniConfig cfg = IniConfig.load(configPath);

        String tenantId = cfg.get("auth", "tenantId", true);
        String clientId = cfg.get("auth", "clientId", true);
        String clientSecret = cfg.get("auth", "clientSecret", true);
        String scope = cfg.getOrDefault("auth", "scope", "https://graph.microsoft.com/.default");

        String mode = cfg.get("operation", "mode", true).trim().toLowerCase();

        log.info("Starting GraphCli");
        log.info("Project dir: {}", projectDir);
        log.info("Config path: {}", configPath);
        log.info("Mode: {}", mode);

        GraphServiceClient graph = buildGraphClient(tenantId, clientId, clientSecret, scope);

        switch (mode) {
            case "search" -> doSearch(cfg, graph);
            case "delete" -> doDelete(cfg, graph);
            case "invite" -> doInvite(cfg, graph);
            default -> throw new IllegalArgumentException("Unknown operation.mode: " + mode + " (expected search|delete|invite)");
        }

        log.info("Done.");
    }

    private static GraphServiceClient buildGraphClient(
            String tenantId,
            String clientId,
            String clientSecret,
            String scope
    ) {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        return new GraphServiceClient(credential, new String[]{scope});
    }

    private static void doSearch(IniConfig cfg, GraphServiceClient graph) {
        String filter = cfg.get("operation", "userSearchFilter", true);

        log.info("Searching users with filter: {}", filter);

        List<User> users = Objects.requireNonNull(
                graph.users().get(request -> {
                    request.queryParameters.filter = filter;
                    request.queryParameters.top = 25;
                    request.queryParameters.select = new String[]{"id", "displayName", "mail", "userPrincipalName"};
                }).getValue()
        );

        log.info("Matched users: {}", users.size());
        System.out.println("Matched users: " + users.size());

        for (User u : users) {
            String line = String.format("- id=%s | displayName=%s | mail=%s | upn=%s",
                    safe(u.getId()), safe(u.getDisplayName()), safe(u.getMail()), safe(u.getUserPrincipalName()));
            log.info(line);
            System.out.println(line);
        }
    }

    private static void doDelete(IniConfig cfg, GraphServiceClient graph) {
        boolean dryRun = cfg.getBoolean("operation", "dryRun", true);
        String lookupBy = cfg.getOrDefault("operation", "lookupBy", "upn").trim().toLowerCase();
        String value = cfg.get("operation", "email", true).trim();

        log.info("Delete requested. dryRun={}, lookupBy={}, value={}", dryRun, lookupBy, value);

        if ("id".equals(lookupBy)) {
            // Direct delete by object id
            if (dryRun) {
                log.warn("DRY RUN: would delete user by id={}", value);
                System.out.println("DRY RUN: would delete user by id=" + value);
                return;
            }
            log.warn("Deleting user by id={}", value);
            graph.users().byUserId(value).delete();
            log.warn("Delete completed for id={}", value);
            System.out.println("Delete completed for id=" + value);
            return;
        }

        // Resolve user by UPN or mail
        String filter;
        if ("upn".equals(lookupBy)) {
            filter = "userPrincipalName eq '" + escapeOData(value) + "'";
        } else if ("mail".equals(lookupBy)) {
            filter = "mail eq '" + escapeOData(value) + "'";
        } else {
            throw new IllegalArgumentException("lookupBy must be one of: upn|mail|id (got: " + lookupBy + ")");
        }

        List<User> users = Objects.requireNonNull(
                graph.users().get(request -> {
                    request.queryParameters.filter = filter;
                    request.queryParameters.top = 5; // allow detection of multiple matches
                    request.queryParameters.select = new String[]{"id", "displayName", "mail", "userPrincipalName"};
                }).getValue()
        );

        if (users.isEmpty()) {
            log.info("No user found for {}={}", lookupBy, value);
            System.out.println("No user found for " + lookupBy + "=" + value);
            return;
        }

        if (users.size() > 1) {
            log.error("Multiple users matched {}={}. Refusing to delete.", lookupBy, value);
            System.out.println("Multiple users matched; refusing to delete. Matches:");
            for (User u : users) {
                String line = String.format("- id=%s | displayName=%s | mail=%s | upn=%s",
                        safe(u.getId()), safe(u.getDisplayName()), safe(u.getMail()), safe(u.getUserPrincipalName()));
                log.error(line);
                System.out.println(line);
            }
            return;
        }

        User target = users.get(0);
        log.warn("Resolved target for delete: id={} displayName={} mail={} upn={}",
                safe(target.getId()), safe(target.getDisplayName()), safe(target.getMail()), safe(target.getUserPrincipalName()));

        if (dryRun) {
            log.warn("DRY RUN: would delete user id={}", safe(target.getId()));
            System.out.println("DRY RUN: would delete user id=" + safe(target.getId()));
            return;
        }

        graph.users().byUserId(target.getId()).delete();
        log.warn("Delete completed for id={}", safe(target.getId()));
        System.out.println("Delete completed.");
    }

    private static void doInvite(IniConfig cfg, GraphServiceClient graph) {
        String email = cfg.get("operation", "email", true).trim();

        String redirectUrl = cfg.get("invite", "redirectUrl", true).trim();
        boolean sendMessage = cfg.getBoolean("invite", "sendInvitationMessage", true);

        log.info("Creating B2B guest invitation for {} redirectUrl={} sendMessage={}", email, redirectUrl, sendMessage);

        Invitation invitation = new Invitation();
        invitation.setInvitedUserEmailAddress(email);
        invitation.setInviteRedirectUrl(redirectUrl);
        invitation.setSendInvitationMessage(sendMessage);

        Invitation created = graph.invitations().post(invitation);

        log.info("Invitation created: invitedUserEmailAddress={} redeemUrl={}",
                safe(created.getInvitedUserEmailAddress()), safe(created.getInviteRedeemUrl()));

        System.out.println("Invitation created.");
        System.out.println("- invitedUserEmailAddress=" + safe(created.getInvitedUserEmailAddress()));
        System.out.println("- inviteRedeemUrl=" + safe(created.getInviteRedeemUrl()));
        if (created.getInvitedUser() != null) {
            System.out.println("- invitedUser.id=" + safe(created.getInvitedUser().getId()));
        }
    }

    private static String escapeOData(String s) {
        return s.replace("'", "''");
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }
}
