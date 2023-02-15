package controllers.core

import play.api.libs.json.Writes
import play.api.mvc.AbstractController

trait SimpleYamlController[A] extends YamlController[A, A] {
  self: AbstractController =>
  implicit val writes: Writes[A]
  override implicit lazy val writesOut: Writes[A] = writes
  override implicit lazy val writesIn: Writes[A] = writes
}
