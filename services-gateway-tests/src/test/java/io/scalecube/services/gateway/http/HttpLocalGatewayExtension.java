package io.scalecube.services.gateway.http;

import io.scalecube.services.gateway.AbstractLocalGatewayExtension;
import io.scalecube.services.gateway.transport.GatewayClientTransports;

class HttpLocalGatewayExtension extends AbstractLocalGatewayExtension {

  private static final String GATEWAY_ALIAS_NAME = "http";

  HttpLocalGatewayExtension(Object serviceInstance) {
    super(
        serviceInstance,
        opts -> new HttpGateway(opts.id(GATEWAY_ALIAS_NAME)),
        GatewayClientTransports::httpGatewayClientTransport);
  }
}
