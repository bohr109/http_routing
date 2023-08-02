package org.ccwang.routing.loadbalancing;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.FakeTicker;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.ccwang.routing.HealthCheckCacheLoader;
import org.junit.Before;
import org.junit.Test;

public class RoundRobinSchemeTest {
	private FakeTicker ticker;
	private LoadingCache<URI, Integer> cache;

	@Before
	public void setup() {
		ticker = new FakeTicker();
		this.cache =
				CacheBuilder.newBuilder()
						.maximumSize(1000)
						.expireAfterWrite(1, TimeUnit.MINUTES)
						.ticker(ticker)
						.build(new HealthCheckCacheLoader());
	}
	
	@Test
	public void testAllServersAvailable_returnsTheNextOne() throws Exception {
		List<MockWebServer> servers = setupMockServers(List.of(200, 200, 200));
		List<URI> uris = servers.stream().map(server -> server.url("/healthz").uri()).toList();
		RoundRobinScheme scheme = new RoundRobinScheme(uris, cache);
		assertThat(scheme.getNextHost()).isEqualTo(Optional.of(uris.get(0)));
		RecordedRequest request1 = servers.get(0).takeRequest();
		assertThat(request1.getRequestUrl()).isEqualTo(servers.get(0).url("/healthz"));

		assertThat(scheme.getNextHost()).isEqualTo(Optional.of(uris.get(1)));
		RecordedRequest request2 = servers.get(1).takeRequest();
		assertThat(request2.getRequestUrl()).isEqualTo(servers.get(1).url("/healthz"));

		assertThat(scheme.getNextHost()).isEqualTo(Optional.of(uris.get(2)));
		RecordedRequest request3 = servers.get(2).takeRequest();
		assertThat(request3.getRequestUrl()).isEqualTo(servers.get(2).url("/healthz"));

		// The next call should hit the cache
		assertThat(scheme.getNextHost()).isEqualTo(Optional.of(uris.get(0)));
		// Server should not be hit again
		RecordedRequest request4 = servers.get(0).takeRequest(100, TimeUnit.MILLISECONDS);
		assertThat(request4).isNull();
	}

	@Test
	public void testSomeServersUnavailable_skipsUnavailableOnes() throws Exception {
		List<MockWebServer> servers = setupMockServers(List.of(200, 503, 200));
		List<URI> uris = servers.stream().map(server -> server.url("/healthz").uri()).toList();
		RoundRobinScheme scheme = new RoundRobinScheme(uris, cache);
		assertThat(scheme.getNextHost()).isEqualTo(Optional.of(uris.get(0)));
		// Should skip the second host
		assertThat(scheme.getNextHost()).isEqualTo(Optional.of(uris.get(2)));
	}

	@Test
	public void testServerUnavailable_comesBackAfterCacheExpiration() throws Exception {
		List<MockWebServer> servers = setupMockServers(List.of(200, 503));
		List<URI> uris = servers.stream().map(server -> server.url("/healthz").uri()).toList();
		RoundRobinScheme scheme = new RoundRobinScheme(uris, cache);

		assertThat(scheme.getNextHost()).isEqualTo(Optional.of(uris.get(0)));
		RecordedRequest request1 = servers.get(0).takeRequest();
		assertThat(request1.getRequestUrl()).isEqualTo(servers.get(0).url("/healthz"));

		// Should skip the second host
		assertThat(scheme.getNextHost()).isEqualTo(Optional.of(uris.get(0)));
		RecordedRequest request2 = servers.get(1).takeRequest();
		assertThat(request2.getRequestUrl()).isEqualTo(servers.get(1).url("/healthz"));

		// Advance ticker to expire cache entry
		ticker.advance(1, TimeUnit.MINUTES);
		servers.get(1).enqueue(new MockResponse().setResponseCode(200));
		assertThat(scheme.getNextHost()).isEqualTo(Optional.of(uris.get(1)));
		// Verifies the second server was hit
		RecordedRequest request3 = servers.get(1).takeRequest();
		assertThat(request3.getRequestUrl()).isEqualTo(servers.get(1).url("/healthz"));
	}

	private List<MockWebServer> setupMockServers(List<Integer> statusCodes) throws IOException {
		ImmutableList.Builder<MockWebServer> servers = ImmutableList.builder();
		for (int statusCode : statusCodes) {
			MockWebServer server = new MockWebServer();
			servers.add(server);
			// Schedule 2 responses.
			server.enqueue(new MockResponse().setResponseCode(statusCode));
			server.start();
		}
		return servers.build();
	}
}
