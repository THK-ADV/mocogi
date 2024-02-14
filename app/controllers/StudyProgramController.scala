package controllers

import database.repo.core.StudyProgramRepository
import database.view.StudyProgramViewRepository
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class StudyProgramController @Inject() (
    cc: ControllerComponents,
    val studyProgramViewRepo: StudyProgramViewRepository,
    val studyProgramRepo: StudyProgramRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def all() =
    Action.async { request =>
      val extend = request
        .getQueryString("extend")
        .flatMap(_.toBooleanOption)
        .getOrElse(false)
      if (extend)
        studyProgramViewRepo.all().map(res => Ok(Json.toJson(res)))
      else
        studyProgramRepo
          .allWithDegrees()
          .map(res =>
            Ok(JsArray(res.map { case (sp, d) =>
              Json.toJsObject(sp).+("degree" -> Json.toJson(d))
            }))
          )
    }
}
