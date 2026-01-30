
package com.keyesit.graphcli;

import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class GraphUserFinder {
  private static final Logger log = LoggerFactory.getLogger(GraphUserFinder.class);

  private static final String[] SELECT = {
      "id",
      "displayName",
      "userPrincipalName",
      "mail",
      "userType",
      "accountEnabled",
      "externalUserState",
      "externalUserStateChangeDateTime"
  };

  private static final Pattern GUID = Pattern
      .compile("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

  private final GraphServiceClient graph;

  public GraphUserFinder(GraphServiceClient graph) {
    this.graph = graph;
  }

  public List<UserSummary> find(AppConfig cfg) {
    String q = cfg.query == null ? "" : cfg.query.trim();
    if (q.isEmpty())
      return List.of();

    // 0) GUID => try by-id first
    if (GUID.matcher(q).matches()) {
      var byId = getOne(q, "AUTO: by-id");
      if (!byId.isEmpty())
        return byId;
    }

    // 1) UPN/email-ish
    if (q.contains("@")) {
      // Try direct lookup first (fast). If it 400s, fall back to filter.
      try {
        var direct = getOne(q, "AUTO: by-UPN direct");
        if (!direct.isEmpty())
          return direct;
      } catch (ApiException e) {
        Integer status = e.getResponseStatusCode();
        if (status != null && status == 400) {
          log.debug("AUTO: UPN direct 400; falling back to filter. {}", e.getMessage());
          // fall through to filters below
        } else if (status != null && status == 404) {
          // not found; continue to filters below (mail etc.)
        } else {
          throw e;
        }
      }

      String esc = escapeOData(q);
      var upn = filter("userPrincipalName eq '" + esc + "'", cfg.maxResults);
      if (!upn.isEmpty())
        return upn;

      return filter("mail eq '" + esc + "'", cfg.maxResults);
    }

    // 2) Name prefix
    return filter("startswith(displayName,'" + escapeOData(q) + "')", cfg.maxResults);
  }

  /** Direct GET by userId (works for id, and often for UPN). */
  private List<UserSummary> getOne(String userId, String tag) {
    log.debug("{} {}", tag, userId);

    try {
      User u = graph.users().byUserId(userId).get(req -> req.queryParameters.select = SELECT);
      return u == null ? List.of() : List.of(toSummary(u));
    } catch (ApiException e) {
      Integer status = e.getResponseStatusCode();
      if (status != null && status == 404)
        return List.of();
      throw e;
    }
  }

  /** Filter query returning up to maxResults. */
  private List<UserSummary> filter(String filter, int maxResults) {
    log.debug("AUTO: by-filter {}", filter);

    var page = graph.users().get(req -> {
      req.queryParameters.filter = filter;
      req.queryParameters.top = maxResults;
      req.queryParameters.select = SELECT;
    });

    List<User> users = Objects.requireNonNullElse(page.getValue(), List.of());
    if (users.isEmpty())
      return List.of();

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
