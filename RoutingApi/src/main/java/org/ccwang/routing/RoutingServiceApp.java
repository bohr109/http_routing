package org.ccwang.routing;

import static io.javalin.apibuilder.ApiBuilder.post;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.javalin.Javalin;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.ccwang.routing.loadbalancing.RoundRobinScheme;

public class RoutingServiceApp {
  private final Javalin app;
  private final RequestDispatcher dispatcher;

  private final LoadingCache<URI, Integer> cache;

  public RoutingServiceApp(List<URI> uris) {
    // TODO: Use dependency injection framework. e.g. Dagger
    this.cache =
        CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new HealthCheckCacheLoader());
    this.dispatcher = new RequestDispatcher(new RoundRobinScheme(uris, cache));
    this.app =
        Javalin.create(
                config -> {
                  config.http.defaultContentType = "application/json";
                })
            .routes(
                () -> {
                  post("/", dispatcher::dispatch);
                });
  }

  public Javalin app() {
    return app;
  }
}
