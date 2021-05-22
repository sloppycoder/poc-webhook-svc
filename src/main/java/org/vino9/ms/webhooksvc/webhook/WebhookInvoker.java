package org.vino9.ms.webhooksvc.webhook;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.vino9.ms.webhooksvc.data.WebhookRequest;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Component
@Slf4j
public class WebhookInvoker {
  public static final int MAX_RETRIES = 10;
  public static final int MAX_INFLIGHT_REQUESTS_PER_CLIENT = 1;

  @Value("${webhook.external-baseurl:http://localhost:9999}")
  private String baseUrl;

  Mono<WebhookRequest> invoke(WebhookRequest request) {
    String messagId = request.getMessageId();
    String clientId = request.getClientId();

    if (request.getStatus() == WebhookRequest.Status.RETRY
        && request.getRetryAfter().isBefore(LocalDateTime.now())) {
      log.info("Too soon to retry request {}. Skip for now", messagId);
      return Mono.empty();
    }

    log.info("Processing request {}", messagId);

    return getWebClient()
        .post()
        .uri(baseUrl + clientId)
        .exchangeToMono(
            response -> {
              if (response.statusCode().is2xxSuccessful()) {
                request.markDone();
              } else {
                markRequestError(request);
              }
              return Mono.just(request);
            })
        .onErrorReturn(WebClientRequestException.class, markRequestError(request));
  }

  private WebhookRequest markRequestError(WebhookRequest request) {
    // TODO: should save response details for reference later
    if (request.getRetries() < MAX_RETRIES) {
      request.markRetryAt(LocalDateTime.now().plus(10, ChronoUnit.SECONDS));
    } else {
      request.markFailed();
    }
    return request;
  }

  private WebClient getWebClient() {

    // use long timeout to prepare for high load test
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
            .responseTimeout(Duration.ofMillis(20_000));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }
}
