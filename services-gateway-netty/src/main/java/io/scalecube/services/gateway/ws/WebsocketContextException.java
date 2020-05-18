package io.scalecube.services.gateway.ws;

import io.scalecube.services.api.ServiceMessage;
import io.scalecube.services.gateway.ReferenceCountUtil;
import java.util.Optional;

public class WebsocketContextException extends RuntimeException {

  private final ServiceMessage request;
  private final ServiceMessage response;

  private WebsocketContextException(
      Throwable cause, ServiceMessage request, ServiceMessage response) {
    super(cause);
    this.request = request;
    this.response = response;
  }

  public static WebsocketContextException badRequest(String errorMessage, ServiceMessage request) {
    return new WebsocketContextException(
        new io.scalecube.services.exceptions.BadRequestException(errorMessage), request, null);
  }

  public static WebsocketContextException wrap(
      Throwable th, ServiceMessage request, ServiceMessage response) {
    return new WebsocketContextException(th, request, response);
  }

  public ServiceMessage request() {
    return request;
  }

  public ServiceMessage response() {
    return response;
  }

  /**
   * Releases request data if any.
   *
   * @return self
   */
  public WebsocketContextException releaseRequest() {
    Optional.ofNullable(request)
        .map(ServiceMessage::data)
        .ifPresent(ReferenceCountUtil::safestRelease);
    return this;
  }
}
