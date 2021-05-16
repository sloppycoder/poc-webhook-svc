package org.vino9.ms.webhooksvc.data;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface WebhookRequestRepository extends ReactiveCrudRepository<WebhookRequest, Long> {
}
