package database.repo

import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.Json
import slick.jdbc.GetResult
import slick.jdbc.JdbcProfile
import webhook.WebhookEndpoint
import webhook.WebhookEvent

/**
 * Journal of accepted GitLab webhooks, keyed by `webhook-id`.
 *
 * Serves deduplication and manual replay only, hence entries carry no processing status.
 */
@Singleton
final class WebhookInboxRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  private given GetResult[WebhookEvent] =
    GetResult(r =>
      WebhookEvent(UUID.fromString(r.nextString()), WebhookEndpoint.fromId(r.nextString()), Json.parse(r.nextString()))
    )

  /**
   * Stores the event unless its `webhook-id` is already stored.
   *
   * GitLab keeps the `webhook-id` for retries and "Resend Request", so repeated deliveries must not be processed twice.
   *
   * @return true if stored, false if it is a duplicate
   */
  def accept(event: WebhookEvent): Future[Boolean] =
    db.run(sql"""
      INSERT INTO modules.webhook_inbox (event_id, endpoint, payload)
      VALUES (CAST(${event.id.toString} AS uuid), ${event.endpoint.id}, CAST(${Json.stringify(event.payload)} AS jsonb))
      ON CONFLICT (event_id) DO NOTHING
    """.asUpdate.map(_ == 1))

  /**
   * Returns the stored event of `eventId`.
   *
   * @return None if the `webhook-id` was never stored, e.g. because the delivery never reached the backend
   */
  def find(eventId: UUID): Future[Option[WebhookEvent]] =
    db.run(sql"""
      SELECT event_id::text, endpoint, payload::text
      FROM modules.webhook_inbox
      WHERE event_id = CAST(${eventId.toString} AS uuid)
    """.as[WebhookEvent].headOption)

  /**
   * Deletes entries received longer than `retention` ago.
   *
   * Such entries serve neither deduplication nor replay but still contain personal data.
   */
  def deleteExpired(retention: Duration): Future[Int] = {
    val cutoff = Timestamp.from(Instant.now().minus(retention))
    db.run(sqlu"DELETE FROM modules.webhook_inbox WHERE received_at < $cutoff")
  }
}
