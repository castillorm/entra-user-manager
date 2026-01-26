package com.yourorg.entra;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.configuration.ConfigurationException;

public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java ... com.yourorg.entra.Main <path-to-ini>");
            System.exit(2);
        }

        String iniPath = args[0];

        IniConfig cfg;
        try {
            cfg = new IniConfig(iniPath);
        } catch (ConfigurationException ce) {
            throw new RuntimeException("Failed to read INI: " + iniPath, ce);
        }

        String tenantId = cfg.getRequired("graph", "tenantId");
        String clientId = cfg.getRequired("graph", "clientId");
        String clientSecret = cfg.getRequired("graph", "clientSecret");
        String authorityHost = cfg.getRequired("graph", "authorityHost");
        String graphBaseUrl = cfg.getRequired("graph", "graphBaseUrl");
        String scope = cfg.getRequired("graph", "scope");
        int timeoutSeconds = cfg.getInt("app", "timeoutSeconds", 30);

        GraphTokenProvider tokenProvider = new GraphTokenProvider(
                authorityHost, tenantId, clientId, clientSecret, scope, timeoutSeconds
        );

        try (GraphHttpClient graph = new GraphHttpClient(tokenProvider, graphBaseUrl, timeoutSeconds)) {
            EntraUserManager mgr = new EntraUserManager(graph);

            // Demo: list first 10 users
            JsonNode users = mgr.listUsers(10);
            System.out.println(users.toPrettyString());

            // Example enable/disable:
            // mgr.setAccountEnabled("user@domain.com", false);
        }
    }
}
