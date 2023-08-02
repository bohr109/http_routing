package org.ccwang.echo;

import io.javalin.Javalin;
import org.ccwang.routing.RequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class SimpleServiceApp {
  private final Javalin app;
  private final Logger logger;

  /**
   * SimpleServiceApp provides 2 API endpoints: 1. /echo to accept HTTP POST and return the response
   * identical to the request body. 2. /healthz to accept GET and return HTTP status code indicating
   * service health status.
   */
  public SimpleServiceApp() {
    logger = LoggerFactory.getLogger(RequestDispatcher.class);
    app =
        Javalin.create(
                config -> {
                  config.http.defaultContentType = "application/json";
                })
            .routes(
                () -> {
                  get(
                      "/healthz",
                      ctx -> {
                        // For now just returns 200 in all cases
                        logger.info("Got health check request from" + ctx.url());
                        ctx.status(200);
                      });
                  post("/echo", EchoController::get);
                });
  }

  public Javalin app() {
    return app;
  }
}
