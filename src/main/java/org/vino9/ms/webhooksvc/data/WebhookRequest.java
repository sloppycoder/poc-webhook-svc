package org.vino9.ms.webhooksvc.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("webhook_requests")
public class WebhookRequest {
  @Id private long id;
  private String messageId;
  private String messageType;
  private String clientId;
  private String payload;
  private Status status;
  private int retries;
  private LocalDateTime retryAfter;
  private LocalDateTime lastUpdated;

  public enum Status {
    NEW,
    LOCKED,
    DONE,
    RETRY,
    FAILED
  }

  public WebhookRequest markDone() {
    setStatus(Status.DONE);
    return this;
  }

  public WebhookRequest markRetryAt(LocalDateTime retryAt) {
    setStatus(Status.RETRY);
    this.retryAfter = retryAt;
    this.retries += 1;
    return this;
  }

  public WebhookRequest markFailed() {
    setStatus(Status.FAILED);
    return this;
  }

  private void setStatus(Status newStatus) {
    LocalDateTime now = LocalDateTime.now();
    this.status = newStatus;
    this.lastUpdated = now;
  }
}
