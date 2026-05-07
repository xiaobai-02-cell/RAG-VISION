package org.example.cvrag.llm;

import org.example.cvrag.config.RagProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

final class DashScopeClientSupport {

    private DashScopeClientSupport() {
    }

    static RestClient create(RagProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getModel().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getModel().getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
