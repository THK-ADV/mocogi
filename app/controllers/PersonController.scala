package controllers

import controllers.json.PersonFormat
import parsing.types.Person
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.PersonService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class PersonController @Inject() (
    cc: ControllerComponents,
    val service: PersonService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with PersonFormat
    with YamlController[Person] {
  override implicit val writes: Writes[Person] = personFormat
}
