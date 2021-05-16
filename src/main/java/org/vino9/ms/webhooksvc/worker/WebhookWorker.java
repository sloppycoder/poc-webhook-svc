package org.vino9.ms.webhooksvc.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.vino9.ms.webhooksvc.data.WebhookRequestRepository;

@Component
@Slf4j
public class WebhookWorker {
  private final WebhookRequestRepository repository;
  private boolean suspended = false;

  @Autowired
  public WebhookWorker(WebhookRequestRepository repository) {
    this.repository = repository;
  }

  @Scheduled(initialDelay = 5000, fixedRate = 3000)
  public void process() {
    if (isSuspended()) {
      log.info("worker suspended");
      return;
    }

    repository.findAll().log().subscribe(r -> log.info("request message id {}", r.getMessageId()));
  }

  public boolean isSuspended() {
    return suspended;
  }

  public void setSuspended(boolean suspended) {
    this.suspended = suspended;
  }
}
