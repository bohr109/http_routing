package org.ccwang.routing.loadbalancing;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load balancing algorithm to dispatch requests in round-robin fashion. There's an auxiliary
 * internal cache to memoize health check results in order not to overwhelm temporarily unavailable
 * hosts.
 */
public class RoundRobinScheme implements LoadBalancingScheme {
  private final ImmutableList<URI> uris;
  private final AtomicInteger counter;
  LoadingCache<URI, Integer> cache;

  public RoundRobinScheme(List<URI> uris, LoadingCache<URI, Integer> cache) {
    this.uris = ImmutableList.copyOf(uris);
    this.counter = new AtomicInteger();
    this.cache = cache;
  }

  @Override
  public synchronized Optional<URI> getNextHost() {
    for (int i = 0; i < uris.size(); ++i) {
      URI candidate = getNextURI();
      if (cache.getUnchecked(candidate).equals(200)) {
        return Optional.of(candidate);
      }
    }
    return Optional.empty();
  }

  private URI getNextURI() {
    // TODO: Launch a background thread to reset the counter
    return uris.get(counter.getAndIncrement() % uris.size());
  }
}
