package webhook

import java.util.UUID

import play.api.libs.json.JsValue

final case class WebhookEvent(id: UUID, endpoint: WebhookEndpoint, payload: JsValue)
