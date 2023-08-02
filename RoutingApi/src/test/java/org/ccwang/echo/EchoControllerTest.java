package org.ccwang.echo;

import com.google.gson.Gson;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class EchoControllerTest {
  private static final String USERS_JSON = new Gson().toJson(List.of("jason", "michael", "robert"));
  private final Context ctx = mock(Context.class);

  @Test
  public void POST_to_returns_echo() {
    when(ctx.body()).thenReturn(USERS_JSON);
    EchoController.get(ctx); // the handler we're testing
    verify(ctx).status(200);
    verify(ctx).result(USERS_JSON);
  }
}
