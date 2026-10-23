package webhook

enum WebhookEndpoint(val id: String, val objectKind: String) {
  case Merge       extends WebhookEndpoint("merge", "merge_request")
  case PushMain    extends WebhookEndpoint("push_main", "push")
  case PushPreview extends WebhookEndpoint("push_preview", "push")
}

object WebhookEndpoint {
  def fromId(id: String): WebhookEndpoint =
    id match {
      case "merge"        => Merge
      case "push_main"    => PushMain
      case "push_preview" => PushPreview
    }
}
