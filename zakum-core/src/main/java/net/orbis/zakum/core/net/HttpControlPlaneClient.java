package net.orbis.zakum.core.net;

import net.orbis.zakum.api.net.ControlPlaneClient;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Java HttpClient implementation (no extra deps).
 */
public final class HttpControlPlaneClient implements ControlPlaneClient {

  private final URI base;
  private final String apiKey;
  private final HttpClient client;

  private HttpControlPlaneClient(URI base, String apiKey, Executor async) {
    this.base = base;
    this.apiKey = apiKey;

    this.client = HttpClient.newBuilder()
      .executor(async)
      .connectTimeout(Duration.ofSeconds(3))
      .build();
  }

  public static Optional<ControlPlaneClient> fromConfig(Plugin plugin, Executor async) {
    var cfg = plugin.getConfig();

    if (!cfg.getBoolean("controlPlane.enabled", false)) {
      return Optional.empty();
    }

    String baseUrl = cfg.getString("controlPlane.baseUrl", "").trim();
    if (baseUrl.isBlank()) {
      plugin.getLogger().warning("controlPlane.enabled=true but controlPlane.baseUrl is blank. Disabling controlPlane.");
      return Optional.empty();
    }

    String apiKey = cfg.getString("controlPlane.apiKey", "");
    return Optional.of(new HttpControlPlaneClient(URI.create(baseUrl), apiKey, async));
  }

  @Override
  public CompletableFuture<HttpResponse<String>> get(String path, Map<String, String> headers) {
    var req = baseRequest(path, headers).GET().build();
    return client.sendAsync(req, HttpResponse.BodyHandlers.ofString());
  }

  @Override
  public CompletableFuture<HttpResponse<String>> postJson(String path, String json, Map<String, String> headers) {
    var req = baseRequest(path, headers)
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(json == null ? "" : json))
      .build();

    return client.sendAsync(req, HttpResponse.BodyHandlers.ofString());
  }

  private HttpRequest.Builder baseRequest(String path, Map<String, String> headers) {
    var uri = base.resolve(path.startsWith("/") ? path.substring(1) : path);

    var b = HttpRequest.newBuilder()
      .uri(uri)
      .timeout(Duration.ofSeconds(6));

    if (apiKey != null && !apiKey.isBlank()) {
      b.header("Authorization", "Bearer " + apiKey);
    }

    if (headers != null) {
      for (var e : headers.entrySet()) {
        if (e.getKey() != null && e.getValue() != null) b.header(e.getKey(), e.getValue());
      }
    }

    return b;
  }
}
