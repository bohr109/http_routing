package org.ccwang.echo;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.Response;
import org.junit.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class FunctionalTest {
  Javalin app = new SimpleServiceApp().app();
  private static final String USERS_JSON = new Gson().toJson(List.of("jason", "michael", "robert"));

  @Test
  public void GET_returns_ok() {
    JavalinTest.test(
        app,
        (server, client) -> {
          Response response = client.get("/healthz");
          assertThat(response.code()).isEqualTo(200);
        });
  }

  @Test
  public void POST_returns_echo() {
    JavalinTest.test(
        app,
        (server, client) -> {
          Response response = client.post("/echo", USERS_JSON);
          assertThat(response.code()).isEqualTo(200);
          assertThat(response.body().string()).isEqualTo(USERS_JSON);
        });
  }
}
