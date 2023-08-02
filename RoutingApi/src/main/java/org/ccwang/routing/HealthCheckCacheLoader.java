package org.ccwang.routing;

import com.google.common.cache.CacheLoader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheckCacheLoader extends CacheLoader<URI, Integer> {
  private final Logger logger;

  public HealthCheckCacheLoader() {
    this.logger = LoggerFactory.getLogger(HealthCheckCacheLoader.class);
  }

  @Override
  public Integer load(URI uri){
    var healthzUri = uri.resolve("/healthz");
    var httpClient = HttpClient.newBuilder().build();
    var request =
        HttpRequest.newBuilder()
            .GET()
            .uri(healthzUri)
            .timeout(Duration.ofMillis(100)) // sets 100ms timeout for health checks.
            .build();
    try {
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      logger.info("Loading key: " + uri + " for value: " + response.statusCode());
      return response.statusCode();
    } catch (Exception e) {
      logger.error("Caught exception while loading key: " + uri + "\n" + e);
      // Swallows all exceptions and returns 403 for now.
      return 403;
    }
  }
}
