package com.wildbugs.freshroute.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class KeepAliveService {

    // Render automatically provides this environment variable when deployed
    @Value("${RENDER_EXTERNAL_URL:}")
    private String renderExternalUrl;

    @Value("${app.keep-alive.custom-url:}")
    private String customKeepAliveUrl;

    private final RestClient restClient = RestClient.create();

    /**
     * Executes every 12 minutes (720,000 milliseconds) with an initial delay of 2 minutes.
     * Render free tier spins down after 15 minutes of inactivity; self-pinging resets the inactivity clock.
     */
    @Scheduled(fixedRate = 720000, initialDelay = 120000)
    public void executeKeepAlivePing() {
        String targetBaseUrl = null;

        if (renderExternalUrl != null && !renderExternalUrl.trim().isEmpty()) {
            targetBaseUrl = renderExternalUrl.trim();
        } else if (customKeepAliveUrl != null && !customKeepAliveUrl.trim().isEmpty()) {
            targetBaseUrl = customKeepAliveUrl.trim();
        }

        if (targetBaseUrl != null) {
            String healthUrl = targetBaseUrl.endsWith("/") ? targetBaseUrl + "api/v1/health" : targetBaseUrl + "/api/v1/health";
            try {
                log.info("Render Keep-Alive: Sending self-ping to prevent 15-min idle sleep -> {}", healthUrl);
                String response = restClient.get()
                        .uri(healthUrl)
                        .retrieve()
                        .body(String.class);
                log.info("Render Keep-Alive: Ping successful! Response: {}", response);
            } catch (Exception e) {
                log.warn("Render Keep-Alive: Self-ping attempt caught exception: {}", e.getMessage());
            }
        } else {
            log.debug("Render Keep-Alive: RENDER_EXTERNAL_URL not set (running locally). Ping skipped.");
        }
    }
}
