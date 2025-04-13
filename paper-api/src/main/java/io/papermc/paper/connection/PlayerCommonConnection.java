package io.papermc.paper.connection;

// TODO: Naming?
public interface PlayerCommonConnection extends CookieConnection {

    void transfer(String host, int port);

    String getBrand();
}
