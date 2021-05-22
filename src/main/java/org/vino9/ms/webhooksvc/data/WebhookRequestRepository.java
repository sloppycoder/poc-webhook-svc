package org.vino9.ms.webhooksvc.data;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface WebhookRequestRepository extends ReactiveCrudRepository<WebhookRequest, Long> {
  @Query(
      "select * from webhook_requests where status = 'NEW' or status = 'RETRY'"
          + "  order by created_at asc ")
  Flux<WebhookRequest> findPendingRequests();
}
