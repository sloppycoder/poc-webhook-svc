package org.vino9.ms.webhooksvc.webhook;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.vino9.ms.webhooksvc.data.WebhookRequest;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebhookInvoker {
  public static final int MAX_RETRIES = 10;
  public static final int MAX_INFLIGHT_REQUESTS_PER_CLIENT = 1;

  private String baseUrl = "http://localhost:9999/";
  private ConcurrentHashMap<String, Integer> inflightRequests = new ConcurrentHashMap<>();

  Mono<WebhookRequest> invoke(WebhookRequest request) {
    String messagId = request.getMessageId();
    String clientId = request.getClientId();

    int count = getInflightRequestsCount(clientId);
    if (count >= MAX_INFLIGHT_REQUESTS_PER_CLIENT) {
      log.info(
          "Client {} has request inflight, skipping {} for now...",
          clientId,
          request.getMessageId());
      return Mono.empty();
    }
    markInflightRequest(clientId);
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
        .onErrorReturn(WebClientRequestException.class, markRequestError(request))
        .doFinally(t -> releaseInflightRequest(clientId));
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

  synchronized void markInflightRequest(String clientId) {
    int count = getInflightRequestsCount(clientId) + 1;
    inflightRequests.put(clientId, count);
    log.debug("markInflightRequest {} to {}", clientId, count);
  }

  synchronized void releaseInflightRequest(String clientId) {
    int count = getInflightRequestsCount(clientId);
    if (count > 0) {
      count = 0;
    }
    inflightRequests.put(clientId, count);
    log.debug("releaseInflightRequest {}, {}}", clientId, count);
  }

  synchronized int getInflightRequestsCount(String clientId) {
    if (inflightRequests.containsKey(clientId)) {
      return inflightRequests.get(clientId);
    } else {
      inflightRequests.put(clientId, 0);
      return 0;
    }
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }
}
