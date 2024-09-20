package models

import models.core.Identity
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class Examiner[A](first: A, second: A)

object Examiner {
  type Default = Examiner[Identity]
  type ID      = Examiner[String]

  implicit def writes: Writes[Default] = Json.writes

  implicit def format: Format[ID] = Json.format

  lazy val NN = Examiner(Identity.NN.id, Identity.NN.id)
}
