package com.yourorg.entra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2 client-credentials token acquisition for Microsoft identity platform v2.
 */
public final class GraphTokenProvider {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final RequestConfig requestConfig;

    // simple in-memory cache
    private String cachedAccessToken;
    private Instant cachedExpiry = Instant.EPOCH;

    public GraphTokenProvider(String authorityHost, String tenantId,
                              String clientId, String clientSecret,
                              String scope, int timeoutSeconds) {

        String base = authorityHost.endsWith("/") ? authorityHost.substring(0, authorityHost.length() - 1) : authorityHost;
        this.tokenEndpoint = base + "/" + tenantId + "/oauth2/v2.0/token";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;

        int timeoutMs = Math.max(1, timeoutSeconds) * 1000;
        this.requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .build();
    }

    public synchronized String getAccessToken() throws Exception {
        // refresh 60s early
        if (cachedAccessToken != null && Instant.now().isBefore(cachedExpiry.minusSeconds(60))) {
            return cachedAccessToken;
        }

        HttpPost post = new HttpPost(tokenEndpoint);
        post.setConfig(requestConfig);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("client_id", clientId));
        form.add(new BasicNameValuePair("client_secret", clientSecret));
        form.add(new BasicNameValuePair("grant_type", "client_credentials"));
        form.add(new BasicNameValuePair("scope", scope));
        post.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));

        try (CloseableHttpClient http = HttpClients.createDefault();
             CloseableHttpResponse resp = http.execute(post)) {

            int code = resp.getStatusLine().getStatusCode();
            String body = resp.getEntity() == null ? "" : EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);

            if (code < 200 || code >= 300) {
                throw new RuntimeException("Token request failed: HTTP " + code + " body=" + body);
            }

            JsonNode json = MAPPER.readTree(body);
            String token = json.path("access_token").asText(null);
            long expiresIn = json.path("expires_in").asLong(0);

            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Token response missing access_token. body=" + body);
            }

            this.cachedAccessToken = token;
            this.cachedExpiry = Instant.now().plusSeconds(Math.max(60, expiresIn));
            return token;
        }
    }
}
