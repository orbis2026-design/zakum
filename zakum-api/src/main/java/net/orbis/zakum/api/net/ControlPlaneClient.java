package net.orbis.zakum.api.net;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ControlPlaneClient {

  CompletableFuture<HttpResponse<String>> get(String path, Map<String, String> headers);

  CompletableFuture<HttpResponse<String>> postJson(String path, String json, Map<String, String> headers);
}
