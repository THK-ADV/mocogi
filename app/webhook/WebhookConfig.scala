package webhook

import java.time.Duration
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

import play.api.Configuration

final case class WebhookConfig(
    projectId: Int,
    signingKeys: Map[WebhookEndpoint, SecretKeySpec],
    timestampTolerance: Duration,
    maxBodyBytes: Long,
    inboxRetention: Duration
) {
  require(
    inboxRetention.toDays >= 2,
    "webhook retention must cover GitLab's two-day window for finding and resending deliveries"
  )
}

object WebhookConfig {
  def load(configuration: Configuration): WebhookConfig = {
    val signingKeys = WebhookEndpoint.values.map { endpoint =>
      val path  = s"git.webhook.signingTokens.${endpoint.id}"
      val token = configuration.get[String](path)
      require(token.startsWith("whsec_"), s"$path must be a GitLab signing token starting with whsec_")
      val key = Base64.getDecoder.decode(token.stripPrefix("whsec_"))
      require(key.length == 32, s"$path must encode 32 bytes")
      endpoint -> new SecretKeySpec(key, "HmacSHA256")
    }.toMap
    WebhookConfig(
      projectId = configuration.get[Int]("git.projectId"),
      signingKeys = signingKeys,
      timestampTolerance = configuration.get[Duration]("git.webhook.timestampTolerance"),
      maxBodyBytes = configuration.get[Long]("git.webhook.maxBodyBytes"),
      inboxRetention = configuration.get[Duration]("git.webhook.retention")
    )
  }
}
