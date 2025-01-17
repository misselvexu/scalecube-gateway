package io.scalecube.services.gateway;

import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;

public abstract class GatewayTemplate implements Gateway {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayTemplate.class);

  protected final GatewayOptions options;
  protected final GatewayMetrics gatewayMetrics;

  protected GatewayTemplate(GatewayOptions options) {
    this.options = new GatewayOptions(options);
    this.gatewayMetrics = new GatewayMetrics(this.options.id(), this.options.metrics());
  }

  @Override
  public final String id() {
    return options.id();
  }

  /**
   * Builds generic http server with given parameters.
   *
   * @param loopResources loop resources
   * @param port listen port
   * @param metrics gateway metrics
   * @return http server
   */
  protected HttpServer prepareHttpServer(
      LoopResources loopResources, int port, GatewayMetrics metrics) {
    return HttpServer.create()
        .tcpConfiguration(
            tcpServer -> {
              if (loopResources != null) {
                tcpServer = tcpServer.runOn(loopResources);
              }
              if (metrics != null) {
                tcpServer =
                    tcpServer.doOnConnection(
                        connection -> {
                          metrics.incConnection();
                          connection.onDispose(metrics::decConnection);
                        });
              }
              return tcpServer.addressSupplier(() -> new InetSocketAddress(port));
            });
  }

  /**
   * Shutting down loopResources if it's not null.
   *
   * @return mono handle
   */
  protected final Mono<Void> shutdownLoopResources(LoopResources loopResources) {
    return Mono.defer(
        () -> {
          if (loopResources == null) {
            return Mono.empty();
          }
          return loopResources
              .disposeLater()
              .doOnError(e -> LOGGER.warn("Failed to close loopResources: " + e));
        });
  }

  /**
   * Shutting down server of type {@link DisposableServer} if it's not null.
   *
   * @param server server
   * @return mono hanle
   */
  protected final Mono<Void> shutdownServer(DisposableServer server) {
    return Mono.defer(
        () -> {
          if (server == null) {
            return Mono.empty();
          }
          server.dispose();
          return server.onDispose().doOnError(e -> LOGGER.warn("Failed to close server: " + e));
        });
  }
}
