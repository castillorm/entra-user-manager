package com.keyesit.graphcli;

import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GraphUserFinder {
  private static final Logger log = LoggerFactory.getLogger(GraphUserFinder.class);

  private final GraphServiceClient graph;

  public GraphUserFinder(GraphServiceClient graph) {
    this.graph = graph;
  }

  public List<UserSummary> find(AppConfig cfg) {
    return switch (cfg.subMode) {
      case id -> findById(cfg.query);
      case upn -> findByUpn(cfg.query);
      case email -> findByFilter("mail eq '" + escapeOData(cfg.query) + "'", cfg.maxResults);
      case namePrefix -> findByFilter("startswith(displayName,'" + escapeOData(cfg.query) + "')", cfg.maxResults);
      case auto -> findAuto(cfg);
    };
  }

  private List<UserSummary> findByUpn(String upn) {
    String q = upn.trim();
    log.debug("Finding by UPN (direct): {}", q);

    // Direct addressing: GET /users/{userPrincipalName}
    // In the Graph SDK, byUserId() accepts either an object id OR a UPN.
    try {

      User u = graph.users().byUserId(q).get(request -> {
        request.queryParameters.select = new String[] {

            "id",
            "displayName",
            "userPrincipalName",
            "mail",
            "userType",
            "accountEnabled",
            "externalUserState",
            "externalUserStateChangeDateTime"
        };
      });

      if (u != null)
        return List.of(toSummary(u));
    } catch (Exception e) {
      // Fall back to filter if direct addressing fails (404, etc.)
      log.debug("Direct UPN lookup did not return a user; falling back to filter. Reason: {}", e.getMessage());
    }

    // Fallback: filter-based exact match
    return findByFilter("userPrincipalName eq '" + escapeOData(q) + "'", 5);
  }

  private List<UserSummary> findAuto(AppConfig cfg) {
    String q = cfg.query.trim();

    // 0) If it looks like an object id (GUID), try by-id first
    if (looksLikeGuid(q)) {
      log.debug("AUTO: query looks like GUID; trying id lookup first");
      List<UserSummary> byId = findById(q);
      if (!byId.isEmpty())
        return byId;

      // If the GUID lookup returns nothing, fall through to other strategies
      log.debug("AUTO: id lookup returned 0; falling back to other strategies");
    }

    // 1) If it contains '@', treat as UPN/email
    if (q.contains("@")) {
      log.debug("AUTO: treating query as UPN/email; trying UPN first then mail");

      List<UserSummary> upn = findByFilter("userPrincipalName eq '" + escapeOData(q) + "'", cfg.maxResults);
      if (!upn.isEmpty())
        return upn;

      return findByFilter("mail eq '" + escapeOData(q) + "'", cfg.maxResults);
    }

    // 2) Otherwise treat as name prefix
    log.debug("AUTO: treating query as namePrefix");
    return findByFilter("startswith(displayName,'" + escapeOData(q) + "')", cfg.maxResults);
  }

  private static boolean looksLikeGuid(String s) {
    // Strict UUID/GUID format: 8-4-4-4-12 hex
    return s.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
  }

  private List<UserSummary> findById(String id) {
    log.debug("Finding by id: {}", id);

    // GET /users/{id}
    User u = graph.users().byUserId(id).get(request -> {
      request.queryParameters.select = new String[] {
          "id",
          "displayName",
          "userPrincipalName",
          "mail",
          "userType",
          "accountEnabled",
          "externalUserState",
          "externalUserStateChangeDateTime"
      };
    });

    if (u == null)
      return List.of();
    return List.of(toSummary(u));
  }

  private List<UserSummary> findByFilter(String filter, int maxResults) {
    log.debug("Finding by filter: {}", filter);

    var page = graph.users().get(request -> {
      request.queryParameters.filter = filter;
      request.queryParameters.top = maxResults;
      request.queryParameters.select = new String[] {
          "id",
          "displayName",
          "userPrincipalName",
          "mail",
          "userType",
          "accountEnabled",
          "externalUserState",
          "externalUserStateChangeDateTime"
      };
    });

    List<User> users = Objects.requireNonNullElse(page.getValue(), List.of());

    List<UserSummary> out = new ArrayList<>(users.size());
    for (User u : users)
      out.add(toSummary(u));
    return out;
  }

  private static UserSummary toSummary(User u) {
    return new UserSummary(
        u.getId(),
        u.getDisplayName(),
        u.getUserPrincipalName(),
        u.getMail(),
        u.getUserType(),
        u.getAccountEnabled(),
        u.getExternalUserState(),
        u.getExternalUserStateChangeDateTime());
  }

  private static String escapeOData(String s) {
    return s.replace("'", "''");
  }
}
