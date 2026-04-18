# Event Delivery Monitoring

Use these signals to detect routing or consumer failures for cross-service platform events:

- Alert when `app.outbox.failed.count > 0` for 5 minutes.
- Alert when `app.outbox.oldest.pending.age.seconds > 300` for 5 minutes.
- Alert when RabbitMQ queue `ai-gateway.knowledge-sync.dlq` has a depth greater than `0` for 5 minutes.

If broker-level unroutable message metrics are not available, alert on app-server log lines that match both:

- `event=platform_outbox_publish_failed`
- `Rabbit message was returned`
