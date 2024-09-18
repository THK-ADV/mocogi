package controllers

import models.core.ExamPhases.ExamPhase
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{
  AbstractController,
  AnyContent,
  ControllerComponents,
  Request
}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

final class ExamPhaseController @Inject() (
    cc: ControllerComponents,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with I18nSupport
    with NelWrites {
  def all() = Action { (r: Request[AnyContent]) =>
    val messages = r.messages
    Ok(
      Json.toJson(
        ExamPhase.all.map(e =>
          Json.obj("id" -> e.id, "label" -> messages(s"exam_phase.${e.id}"))
        )
      )
    )
  }
}
