package org.ccwang.routing.loadbalancing;

import java.net.URI;
import java.util.Optional;

/** Interface for the algorithm of load balancing. */
public interface LoadBalancingScheme {
  Optional<URI> getNextHost();
}
