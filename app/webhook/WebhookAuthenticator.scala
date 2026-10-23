package webhook

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

import cats.syntax.either.*
import org.apache.pekko.util.ByteString
import play.api.mvc.Headers
import webhook.WebhookAuthenticator.Rejection

/** Verifies signed GitLab webhook deliveries. */
final class WebhookAuthenticator(config: WebhookConfig) {

  /**
   * Verifies the `webhook-id`, `webhook-timestamp` and `webhook-signature` headers against the raw body.
   *
   * Rejects timestamps outside the configured tolerance to prevent replay attacks.
   *
   * @return the `webhook-id`, or the rejection reason
   */
  def verify(endpoint: WebhookEndpoint, headers: Headers, body: ByteString): Either[Rejection, UUID] =
    for {
      id        <- header(headers, "webhook-id")
      eventId   <- Either.catchOnly[IllegalArgumentException](UUID.fromString(id)).leftMap(_ => Rejection.InvalidEventId)
      timestamp <- header(headers, "webhook-timestamp")
      seconds   <- timestamp.toLongOption.toRight(Rejection.InvalidTimestamp)
      signature <- header(headers, "webhook-signature")
      now       = Instant.now().getEpochSecond
      tolerance = config.timestampTolerance.toSeconds
      _ <- Either.raiseUnless(seconds >= now - tolerance && seconds <= now + tolerance)(Rejection.ExpiredTimestamp)
      _ <- Either.raiseUnless(verifySignature(config.signingKeys(endpoint), id, timestamp, body, signature))(
        Rejection.InvalidSignature
      )
    } yield eventId

  private def header(headers: Headers, name: String): Either[Rejection, String] =
    headers.getAll(name) match {
      case Seq(value) if value.nonEmpty => Right(value)
      case _                            => Left(Rejection.MissingOrAmbiguousHeader(name))
    }

  /**
   * Checks whether any `v1` signature in `signatures` matches the HMAC-SHA256 of `id.timestamp.body`.
   *
   * Signs the raw body since reserializing JSON would change the signed bytes, and compares in
   * constant time to prevent timing attacks.
   */
  private def verifySignature(
      key: SecretKeySpec,
      id: String,
      timestamp: String,
      body: ByteString,
      signatures: String
  ): Boolean = {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(key)
    mac.update(s"$id.$timestamp.".getBytes(UTF_8))
    val expected = mac.doFinal(body.toArray)
    signatures.split(" ").exists { signature =>
      signature.startsWith("v1,") &&
      (try MessageDigest.isEqual(expected, Base64.getDecoder.decode(signature.drop(3)))
      catch { case _: IllegalArgumentException => false })
    }
  }
}

object WebhookAuthenticator {
  enum Rejection {
    case MissingOrAmbiguousHeader(name: String)
    case InvalidEventId, InvalidTimestamp, ExpiredTimestamp, InvalidSignature
  }
}
