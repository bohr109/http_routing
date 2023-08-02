package org.ccwang.echo;

public class Main {
  public static void main(String[] args) {
    new SimpleServiceApp().app().start(Integer.parseInt(args[0]));
  }
}
