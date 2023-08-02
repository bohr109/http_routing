package org.ccwang.routing;

import com.google.common.collect.ImmutableList;
import java.net.URI;

public class Main {
  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("Expecting argument list: <port> <host1> <host2> ...");
      System.exit(0);
    }
    ImmutableList.Builder<URI> builder = ImmutableList.builder();
    for (int i = 1; i < args.length; ++i) {
      builder.add(URI.create(args[i]));
    }


    new RoutingServiceApp(builder.build()).app().start(Integer.parseInt(args[0]));
  }
}
