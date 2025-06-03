package controllers

import javax.inject.Inject

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import models.core.ExamPhases.ExamPhase
import play.api.cache.Cached
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents

final class ExamPhaseController @Inject() (
    cc: ControllerComponents,
    cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with I18nSupport
    with NelWrites {
  def all() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action { r =>
        val messages = r.messages
        Ok(
          Json.toJson(
            ExamPhase.all.map(e =>
              Json.obj(
                "id"     -> e.id,
                "label"  -> messages(s"exam_phase.label.${e.id}"),
                "abbrev" -> messages(s"exam_phase.short.${e.id}")
              )
            )
          )
        )
      }
    }
}
