package io.scalecube.services.gateway.rsocket;

import io.netty.channel.EventLoopGroup;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.ByteBufPayload;
import io.scalecube.net.Address;
import io.scalecube.services.ServiceCall;
import io.scalecube.services.gateway.Gateway;
import io.scalecube.services.gateway.GatewayLoopResources;
import io.scalecube.services.gateway.GatewayOptions;
import io.scalecube.services.gateway.GatewayTemplate;
import io.scalecube.services.gateway.ReferenceCountUtil;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;

public class RSocketGateway extends GatewayTemplate {

  private static final Logger LOGGER = LoggerFactory.getLogger(RSocketGateway.class);

  private CloseableChannel server;
  private LoopResources loopResources;

  public RSocketGateway(GatewayOptions options) {
    super(options);
  }

  @Override
  public Mono<Gateway> start() {
    return Mono.defer(
        () -> {
          ServiceCall serviceCall =
              options.call().requestReleaser(ReferenceCountUtil::safestRelease);
          RSocketGatewayAcceptor acceptor = new RSocketGatewayAcceptor(serviceCall, gatewayMetrics);

          if (options.workerPool() != null) {
            loopResources = new GatewayLoopResources((EventLoopGroup) options.workerPool());
          } else {
            loopResources = LoopResources.create("rsocket-gateway");
          }

          WebsocketServerTransport rsocketTransport =
              WebsocketServerTransport.create(
                  prepareHttpServer(loopResources, options.port(), gatewayMetrics));

          return RSocketFactory.receive()
              .frameDecoder(
                  frame ->
                      ByteBufPayload.create(
                          frame.sliceData().retain(), frame.sliceMetadata().retain()))
              .acceptor(acceptor)
              .transport(rsocketTransport)
              .start()
              .doOnSuccess(server -> this.server = server)
              .thenReturn(this);
        });
  }

  @Override
  public Address address() {
    InetSocketAddress address = server.address();
    return Address.create(address.getHostString(), address.getPort());
  }

  @Override
  public Mono<Void> stop() {
    return Flux.concatDelayError(shutdownServer(), shutdownLoopResources(loopResources)).then();
  }

  private Mono<Void> shutdownServer() {
    return Mono.defer(
        () -> {
          if (server == null) {
            return Mono.empty();
          }
          server.dispose();
          return server.onClose().doOnError(e -> LOGGER.warn("Failed to close server: " + e));
        });
  }
}
