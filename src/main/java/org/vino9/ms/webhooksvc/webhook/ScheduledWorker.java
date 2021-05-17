package org.vino9.ms.webhooksvc.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.vino9.ms.webhooksvc.data.WebhookRequestRepository;

@Service
@Slf4j
public class ScheduledWorker {
  private final WebhookRequestRepository repository;
  private final WebhookInvoker client;
  private boolean suspended = false;

  @Autowired
  public ScheduledWorker(WebhookRequestRepository repository, WebhookInvoker client) {
    this.repository = repository;
    this.client = client;
  }

  @Scheduled(initialDelayString = "${webhook.scheduler.init-delay:5000}", fixedDelayString = "${webhook.scheduler.init-delay:5000}")
  public void process() {
    if (isSuspended()) {
      log.info("worker suspended");
      return;
    }
    repository
        .findPendingRequests()
        .map(client::invoke)
        .flatMap(s -> s)
        .flatMap(repository::save)
        .subscribe();
  }

  public boolean isSuspended() {
    return suspended;
  }

  public void suspend() {
    this.suspended = true;
  }

  public void resume() {
    this.suspended = false;
  }
}
