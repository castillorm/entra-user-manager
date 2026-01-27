package com.yourorg.entra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;

public final class GraphHttpClient implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CloseableHttpClient http;
    private final GraphTokenProvider tokenProvider;
    private final String graphBaseUrl;
    private final RequestConfig requestConfig;

    public GraphHttpClient(GraphTokenProvider tokenProvider, String graphBaseUrl, int timeoutSeconds) {
        this.tokenProvider = tokenProvider;
        this.graphBaseUrl = graphBaseUrl.endsWith("/") ? graphBaseUrl.substring(0, graphBaseUrl.length() - 1) : graphBaseUrl;

        int timeoutMs = Math.max(1, timeoutSeconds) * 1000;
        this.requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .build();

        this.http = HttpClients.createDefault();
    }

    public JsonNode get(String pathAndQuery) throws Exception {
        HttpGet req = new HttpGet(graphBaseUrl + pathAndQuery);
        return executeJson(req, null);
    }

    public JsonNode postJson(String pathAndQuery, String jsonBody) throws Exception {
        HttpPost req = new HttpPost(graphBaseUrl + pathAndQuery);
        req.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        return executeJson(req, "application/json");
    }

    public JsonNode patchJson(String pathAndQuery, String jsonBody) throws Exception {
        HttpPatch req = new HttpPatch(graphBaseUrl + pathAndQuery);
        req.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        return executeJson(req, "application/json");
    }

    public void delete(String pathAndQuery) throws Exception {
        HttpDelete req = new HttpDelete(graphBaseUrl + pathAndQuery);
        executeNoBody(req);
    }

    private JsonNode executeJson(HttpRequestBase req, String contentType) throws Exception {
        req.setConfig(requestConfig);
        req.setHeader("Authorization", "Bearer " + tokenProvider.getAccessToken());
        req.setHeader("Accept", "application/json");
        if (contentType != null) req.setHeader("Content-Type", contentType);

        try (CloseableHttpResponse resp = http.execute(req)) {
            int code = resp.getStatusLine().getStatusCode();
            String body = resp.getEntity() == null ? "" : EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);

            if (code < 200 || code >= 300) {
                throw new RuntimeException("Graph call failed: " + req.getMethod() + " " + req.getURI()
                        + " HTTP " + code + " body=" + body);
            }
            if (body.isEmpty()) {
                return MAPPER.createObjectNode();
            }
            return MAPPER.readTree(body);
        }
    }

    private void executeNoBody(HttpRequestBase req) throws Exception {
        req.setConfig(requestConfig);
        req.setHeader("Authorization", "Bearer " + tokenProvider.getAccessToken());
        req.setHeader("Accept", "application/json");

        try (CloseableHttpResponse resp = http.execute(req)) {
            int code = resp.getStatusLine().getStatusCode();
            String body = resp.getEntity() == null ? "" : EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);

            if (code < 200 || code >= 300) {
                throw new RuntimeException("Graph call failed: " + req.getMethod() + " " + req.getURI()
                        + " HTTP " + code + " body=" + body);
            }
        }
    }

    @Override
    public void close() throws Exception {
        http.close();
    }
}
