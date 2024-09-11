package controllers

import models.core.ExamPhase
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

final class ExamPhaseController @Inject() (
    cc: ControllerComponents,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with I18nSupport
    with NelWrites {
  def all() = Action { request =>
    val messages = request.messages
    Ok(
      Json.toJson(
        ExamPhase.all.map(e =>
          Json.obj("id" -> e.id, "label" -> messages(s"exam_phase.${e.id}"))
        )
      )
    )
  }
}
