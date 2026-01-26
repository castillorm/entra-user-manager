package com.yourorg.entra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class EntraUserManager {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final GraphHttpClient graph;

    public EntraUserManager(GraphHttpClient graph) {
        this.graph = graph;
    }

    /** List users (basic fields). Requires Directory.Read.All or User.Read.All application permission. */
    public JsonNode listUsers(int top) throws Exception {
        int safeTop = Math.max(1, Math.min(top, 999));
        String path = "/v1.0/users?$top=" + safeTop + "&$select=id,displayName,userPrincipalName,accountEnabled";
        return graph.get(path);
    }

    /** Get a user by UPN or objectId. */
    public JsonNode getUser(String userIdOrUpn) throws Exception {
        String path = "/v1.0/users/" + urlEncodePath(userIdOrUpn) + "?$select=id,displayName,userPrincipalName,accountEnabled";
        return graph.get(path);
    }

    /** Disable (or enable) a user. Requires User.ReadWrite.All application permission. */
    public void setAccountEnabled(String userIdOrUpn, boolean enabled) throws Exception {
        String path = "/v1.0/users/" + urlEncodePath(userIdOrUpn);
        String body = MAPPER.createObjectNode().put("accountEnabled", enabled).toString();
        graph.patchJson(path, body);
    }

    private static String urlEncodePath(String s) {
        // Minimal safe encoding for path segment; avoids adding new deps.
        return s.replace(" ", "%20").replace("@", "%40");
    }
}
