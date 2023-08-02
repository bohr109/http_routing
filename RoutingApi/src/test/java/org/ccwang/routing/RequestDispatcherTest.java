package org.ccwang.routing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import io.javalin.http.Context;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.ccwang.routing.loadbalancing.LoadBalancingScheme;
import org.junit.jupiter.api.Test;

public class RequestDispatcherTest {
  private static final String JSON_PAYLOAD =
      new Gson().toJson(List.of("jason", "michael", "robert"));

  @Test
  public void testRoundRobinDispatching() throws Exception {
    List<MockWebServer> servers = setupMockServers(3);

    RequestDispatcher dispatcher =
        new RequestDispatcher(
            new SimpleRoundRobinScheme(
                servers.stream().map(server -> server.url("/echo").uri()).toList()));
    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn(JSON_PAYLOAD);

    dispatcher.dispatch(ctx);
    RecordedRequest request1 = servers.get(0).takeRequest();
    assertThat(request1.getRequestUrl()).isEqualTo(servers.get(0).url("/echo"));

    dispatcher.dispatch(ctx);
    RecordedRequest request2 = servers.get(1).takeRequest();
    assertThat(request2.getRequestUrl()).isEqualTo(servers.get(1).url("/echo"));

    dispatcher.dispatch(ctx);
    RecordedRequest request3 = servers.get(2).takeRequest();
    assertThat(request3.getRequestUrl()).isEqualTo(servers.get(2).url("/echo"));

    verify(ctx, times(3)).status(200);
    verify(ctx, times(3)).result(JSON_PAYLOAD);
    shutdownServers(servers);
  }

  @Test
  public void testDispatchingSkipsFailedServer() throws Exception {
    List<MockWebServer> servers = setupMockServers(2);

    RequestDispatcher dispatcher =
        new RequestDispatcher(
            new SimpleRoundRobinScheme(
                servers.stream().map(server -> server.url("/echo").uri()).toList()));
    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn(JSON_PAYLOAD);

    // Shuts down the first server
    servers.get(0).shutdown();

    dispatcher.dispatch(ctx);
    // Should be the second server took the request
    RecordedRequest request2 = servers.get(1).takeRequest();
    assertThat(request2.getRequestUrl()).isEqualTo(servers.get(1).url("/echo"));

    // Shuts down the second server
    servers.get(1).shutdown();
  }

  @Test
  public void testExhaustingRetries() throws Exception {
    List<MockWebServer> servers = setupMockServers(4);

    RequestDispatcher dispatcher =
        new RequestDispatcher(
            new SimpleRoundRobinScheme(
                servers.stream().map(server -> server.url("/echo").uri()).toList()));
    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn(JSON_PAYLOAD);

    // Shuts down all the servers
    shutdownServers(servers);

    dispatcher.dispatch(ctx);
    // Should exhaust the retries
    verify(ctx).status(503);
  }

  @Test
  public void testDispatchingSkipsTimedOutServer() throws Exception {
    MockWebServer server1 = new MockWebServer();
    // Delays 500 ms (The request timeout is 200ms)
    server1.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(JSON_PAYLOAD)
            // Throttle the response body by sleeping 100ms after every 3 bytes
            .throttleBody(3, 100, TimeUnit.MILLISECONDS));
    server1.start();

    MockWebServer server2 = new MockWebServer();
    server2.enqueue(new MockResponse().setResponseCode(200).setBody(JSON_PAYLOAD));
    server2.start();

    RequestDispatcher dispatcher =
        new RequestDispatcher(
            new SimpleRoundRobinScheme(
                List.of(server1.url("/echo").uri(), server2.url("/echo").uri())));
    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn(JSON_PAYLOAD);

    dispatcher.dispatch(ctx);
    // The second server should take the request
    RecordedRequest request2 = server2.takeRequest();
    assertThat(request2.getRequestUrl()).isEqualTo(server2.url("/echo"));

    // Shuts down all the servers
    shutdownServers(List.of(server1, server2));
  }

  private List<MockWebServer> setupMockServers(int numServers) throws IOException {
    ImmutableList.Builder<MockWebServer> servers = ImmutableList.builder();
    for (int i = 0; i < numServers; ++i) {
      MockWebServer server = new MockWebServer();
      servers.add(server);
      // Schedule 2 responses.
      server.enqueue(new MockResponse().setResponseCode(200).setBody(JSON_PAYLOAD));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(JSON_PAYLOAD));
      server.start();
    }
    return servers.build();
  }

  private void shutdownServers(List<MockWebServer> servers) throws IOException {
    for (MockWebServer server : servers) {
      server.shutdown();
    }
  }

  /** Simplified {@link org.ccwang.routing.loadbalancing.RoundRobinScheme} without cache support */
  static class SimpleRoundRobinScheme implements LoadBalancingScheme {
    private final ImmutableList<URI> uris;
    private final AtomicInteger counter;

    public SimpleRoundRobinScheme(List<URI> uris) {
      this.uris = ImmutableList.copyOf(uris);
      this.counter = new AtomicInteger();
    }

    @Override
    public Optional<URI> getNextHost() {
      return Optional.of(uris.get(counter.getAndIncrement() % uris.size()));
    }
  }
}
