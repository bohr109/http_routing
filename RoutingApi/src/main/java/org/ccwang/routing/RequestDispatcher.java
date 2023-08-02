package org.ccwang.routing;

import io.javalin.http.Context;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.ccwang.routing.loadbalancing.LoadBalancingScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestDispatcher {
  private static final int MAX_RETRIES = 2;
  private final HttpClient httpClient;
  private final Logger logger;
  private final LoadBalancingScheme loadBalancingScheme;

  public RequestDispatcher(LoadBalancingScheme loadBalancingScheme) {
    this.httpClient = HttpClient.newBuilder().build();
    this.logger = LoggerFactory.getLogger(RequestDispatcher.class);
    this.loadBalancingScheme = loadBalancingScheme;
  }

  public void dispatch(Context context) {
    int retries = 0;
    do {
      var uriOpt = loadBalancingScheme.getNextHost();
      if (uriOpt.isEmpty()) {
        // No available hosts. Returns 503 Service Unavailable and exits.
        context.status(503);
        return;
      }
      var uri = uriOpt.get();
      try {
        // TODO: Switch to async requests
        var response = forwardHttpRequest(context, uri);
        if (response.statusCode() == 200) {
          context.result(response.body());
          context.status(response.statusCode());
          return; // Exit early upon successful request
        }
        logger.info("Got " + response.statusCode() + " from " + uri);
      } catch (Exception e) {
        logger.error("Got exception from: " + uri + " " + e);
      } finally {
        retries++;
      }
    } while (retries <= MAX_RETRIES);
    // Returns 503 Service Unavailable
    context.status(503);
  }

  private HttpResponse<String> forwardHttpRequest(Context context, URI uri)
      throws IOException, InterruptedException {
    var echoEndPoint = uri.resolve("/echo");
    var request =
        HttpRequest.newBuilder()
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(context.body()))
            .uri(echoEndPoint)
            .timeout(Duration.ofMillis(100)) // sets 100ms timeout
            .build();
    logger.info("Dispatching request to " + echoEndPoint);
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
