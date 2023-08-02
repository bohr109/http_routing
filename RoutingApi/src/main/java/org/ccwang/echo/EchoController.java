package org.ccwang.echo;

import io.javalin.http.Context;
import org.ccwang.routing.RequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoController {
  public static void get(Context context) {
    Logger logger = LoggerFactory.getLogger(RequestDispatcher.class);
    logger.info("Got echo request from" + context.url());
    context.result(context.body()).contentType("application/json");
    context.status(200);
  }
}
