package io.scalecube.services.testservice;

import io.scalecube.services.api.ServiceMessage;
import io.scalecube.services.gateway.GatewaySession;
import io.scalecube.services.gateway.GatewaySessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class SecuredRsGwGatewaySessionHandler implements GatewaySessionHandler<ServiceMessage> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(SecuredRsGwGatewaySessionHandler.class);

  private final AuthRegistry authRegistry;

  public SecuredRsGwGatewaySessionHandler(AuthRegistry authRegistry) {
    this.authRegistry = authRegistry;
  }

  @Override
  public ServiceMessage mapMessage(GatewaySession session, ServiceMessage req, Context context) {
    return ServiceMessage.from(req).header(AuthRegistry.SESSION_ID, session.sessionId()).build();
  }

  @Override
  public Mono<Void> onSessionOpen(GatewaySession s) {
    return Mono.fromRunnable(() -> LOGGER.info("Session opened: {}", s));
  }

  @Override
  public Mono<Void> onSessionClose(GatewaySession session) {
    return Mono.fromRunnable(
        () -> {
          LOGGER.info("Session removed: {}", session);
          authRegistry.removeAuth(session.sessionId());
        });
  }
}
