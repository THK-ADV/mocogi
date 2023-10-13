package controllers.formats

import models.User
import play.api.libs.json.Format

trait UserFormat {
  implicit val userFormat: Format[User] =
    Format.of[String].bimap(User.apply, _.username)
}
