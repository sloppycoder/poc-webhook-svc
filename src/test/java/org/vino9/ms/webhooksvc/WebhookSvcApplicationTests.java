package org.vino9.ms.webhooksvc;

import com.github.tomakehurst.wiremock.client.WireMock;
import de.mkammerer.wiremock.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;

@SpringBootTest
class WebhookSvcApplicationTests {
  @RegisterExtension WireMockExtension wireMock = new WireMockExtension();

  @Test
  void contextLoads() {}

  @Test
  void test() {
    wireMock.stubFor(WireMock.get("/hello").willReturn(WireMock.ok("world")));
    URI uri = wireMock.getBaseUri().resolve("/hello");
  }
}
