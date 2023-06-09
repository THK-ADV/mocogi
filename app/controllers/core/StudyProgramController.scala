package controllers.core

import controllers.formats.{StudyProgramAtomicFormat, StudyProgramFormat}
import database.view.StudyProgramViewRepository
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.StudyProgramService

import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class StudyProgramController @Inject() (
    cc: ControllerComponents,
    val service: StudyProgramService,
    val materializedView: StudyProgramViewRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with StudyProgramFormat
    with StudyProgramAtomicFormat {
  def all() =
    Action.async { _ =>
      service.all().map(xs => Ok(Json.toJson(xs)))
    }

  def create() =
    Action(parse.byteString).async { r =>
      val input = r.body.decodeString(StandardCharsets.UTF_8)
      service.create(input).map(xs => Ok(Json.toJson(xs)))
    }

  def allAtomic() =
    Action.async { _ =>
      materializedView.all().map(res => Ok(Json.toJson(res)))
    }
}
