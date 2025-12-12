package permission

enum Role(val id: String) {
  case NotifyReviewer extends Role("notify-reviewer")
  case UpdateImages   extends Role("update-images")
}
