package org.vino9.ms.webhooksvc.webhook;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.vino9.ms.webhooksvc.data.WebhookRequest;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Component
@Slf4j
public class WebhookInvoker {
  public static final int MAX_RETRIES = 10;
  public static final Map<String, String> ENDPOINTS =
      Map.of(
          "CL001", "http://localhost:9999/cl001",
          "CL002", "http://localhost:9999/cl002",
          "CL003", "http://localhost:9999/cl003");

  Mono<WebhookRequest> invoke(WebhookRequest request) {
    log.info("Processing request {}", request.getMessageId());
    String clientId = request.getClientId();
    return getWebClient()
        .post()
        .uri(ENDPOINTS.get(clientId))
        .exchangeToMono(
            response -> {
              if (response.statusCode().is2xxSuccessful()) {
                request.markDone();
              } else {
                // TODO: should save response details for reference later
                if (request.getRetries() < MAX_RETRIES) {
                  request.markRetryAt(LocalDateTime.now().plus(10, ChronoUnit.SECONDS));
                } else {
                  request.markFailed();
                }
              }
              return Mono.just(request);
            });
  }

  private WebClient getWebClient() {

    // use long timeout to prepare for high load test
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(Duration.ofMillis(3600_000));
    //                .doOnConnected(conn ->
    //                        conn.addHandlerLast(new ReadTimeoutHandler(10000,
    // TimeUnit.MILLISECONDS))
    //                                .addHandlerLast(new WriteTimeoutHandler(10000,
    // TimeUnit.MILLISECONDS)));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
