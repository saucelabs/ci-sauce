package com.saucelabs.ci.sauceconnect;

import org.json.JSONObject;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Semaphore;

/** Monitors SC Process via HTTP API */
public class DefaultSCMonitor implements SCMonitor {
    public static class Factory implements SCMonitorFactory {
        public SCMonitor create(int port, Logger logger) {
            return new DefaultSCMonitor(port, logger);
        }
    }

    private Semaphore semaphore;
    private final int port;
    private final Logger logger;

    private boolean failed;
    private boolean apiResponse;

    private HttpClient client = HttpClient.newHttpClient();
    private static final int sleepTime = 1000;

    private Exception lastHealtcheckException;

    public DefaultSCMonitor(final int port, final Logger logger) {
        this.port = port;
        this.logger = logger;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public String getTunnelId() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:%d/info", port)))
            .GET()
            .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            JSONObject jsonObject = new JSONObject(responseBody);
            if (jsonObject.has("tunnel_id")) {
                return jsonObject.getString("tunnel_id");
            }
        } catch (Exception e) {
            this.logger.info("Failed to get tunnel id", e);
            return null;
        }
        this.logger.info("Failed to get tunnel id");
        return null;
    }

    public boolean isFailed() {
        return failed;
    }

    public Exception getLastHealtcheckException() {
        return lastHealtcheckException;
    }

    public void run() {
        while (true) {
            pollEndpoint();
            if (this.semaphore.availablePermits() > 0) {
                return;
            }

            try {
                Thread.sleep(sleepTime);
            } catch ( java.lang.InterruptedException e ) {
                return;
            }
        }
    }

    protected void pollEndpoint() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format("http://localhost:%d/readyz", port)))
            .GET()
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            this.apiResponse = true;

            if (response.statusCode() == 200) {
                this.logger.info("Health check got connected status");
                this.semaphore.release();
                return;
            }

            this.lastHealtcheckException = new Exception("Invalid API response code: " + response.statusCode());
        } catch ( Exception e ) {
            this.lastHealtcheckException = e;
            if ( this.apiResponse ) {
                // We've had a successful API endpoint read, but then it stopped responding, which means the process failed to start
                this.failed = true;
                this.logger.warn("Health check API stopped responding", e);
                this.semaphore.release();
                return;
            }
        }

        this.logger.trace("Waiting for sauceconnect to be ready err={}", this.lastHealtcheckException.toString());
    }
}
