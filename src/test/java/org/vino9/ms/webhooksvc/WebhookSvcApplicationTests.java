package org.vino9.ms.webhooksvc;

import com.github.tomakehurst.wiremock.client.WireMock;
import de.mkammerer.wiremock.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.SocketUtils;
import org.vino9.ms.webhooksvc.data.WebhookRequest;
import org.vino9.ms.webhooksvc.data.WebhookRequestRepository;
import org.vino9.ms.webhooksvc.webhook.ScheduledWorker;
import org.vino9.ms.webhooksvc.webhook.WebhookInvoker;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebhookSvcApplicationTests {
  @RegisterExtension
  WireMockExtension wireMock = new WireMockExtension(SocketUtils.findAvailableTcpPort());

  @Autowired WebhookInvoker invoker;
  @Autowired ScheduledWorker worker;

  @Autowired WebhookRequestRepository repository;

  @Test
  void contextLoads() {}

  @Test
  void all_requests_successful() throws InterruptedException {
    wireMock.stubFor(WireMock.post(anyUrl()).willReturn(WireMock.ok("OK")));
    URI uri = wireMock.getBaseUri().resolve("/");
    invoker.setBaseUrl(uri.toString());

    // wait for scheduler to start and do its job
    TimeUnit.SECONDS.sleep(4);
    worker.suspend();

    Map<String, WebhookRequest> requests = getAllRequestsAsMap();
    assertEquals(requests.get("11-22-33-44").getStatus(), WebhookRequest.Status.DONE);
    assertEquals(requests.get("12-22-33-44").getStatus(), WebhookRequest.Status.DONE);
  }

  public Map<String, WebhookRequest> getAllRequestsAsMap() {
    return repository.findAll().collectMap(WebhookRequest::getMessageId).block();
  }
}
