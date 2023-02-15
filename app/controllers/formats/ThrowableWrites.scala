package controllers.formats

import play.api.libs.json.{Json, Writes}

trait ThrowableWrites {
  implicit val throwableWrites: Writes[Throwable] =
    Writes.apply(e =>
      Json.obj(
        "type" -> "throwable",
        "message" -> e.getMessage
      )
    )
}
