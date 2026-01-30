package service.schedule

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.Semester
import play.api.libs.json.*
import play.api.libs.ws.WSClient

@Singleton
final class HolidayService @Inject() (ws: WSClient, implicit val ctx: ExecutionContext) {
  def holidaysByNow() = {
    val years = Semester.currentAndNext().map(_.year).mkString(",")
    ws.url(s"https://get.api-feiertage.de?years=$years&states=nw").get().map(resp => parse(resp.json))
  }

  private def parse(js: JsValue): collection.IndexedSeq[JsObject] =
    js.\("feiertage")
      .validate[JsArray]
      .map(
        _.value
          .map { day =>
            for {
              date  <- day.\("date").validate[LocalDate]
              label <- day.\("fname").validate[String]
            } yield Json.obj("label" -> label, "date" -> date)
          }
          .collect { case JsSuccess(a, _) => a }
      )
      .getOrElse(IndexedSeq.empty)
}
