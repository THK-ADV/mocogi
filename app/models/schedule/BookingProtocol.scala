package models.schedule

import java.time.Instant
import java.time.ZoneId
import java.util.UUID

import play.api.libs.json.*

/** Client-editable fields only; createdBy and updatedAt are always supplied by the server. */
case class BookingProtocol(
    kind: BookingKind,
    seriesId: ScheduleEntrySeriesId,
    title: String,
    note: Option[String],
    rooms: List[UUID],
    lecturer: List[String],
    start: Instant,
    end: Instant,
    module: Option[UUID],
    courseType: Option[CourseType],
    po: Option[JsValue]
)

object BookingProtocol {
  private val zone = ZoneId.of("Europe/Berlin")

  private def validPO(value: JsValue): Boolean = value match {
    case JsArray(entries) =>
      entries.nonEmpty && entries.forall { entry =>
        (entry \ "po").validate[String].isSuccess &&
        (entry \ "specialization").validateOpt[String].isSuccess &&
        (entry \ "recommendedSemester").validate[List[Int]].isSuccess &&
        (entry \ "mandatory").validate[Boolean].isSuccess
      }
    case _ => false
  }

  def validTimes(start: Instant, end: Instant): Boolean =
    end.isAfter(start) && start.atZone(zone).toLocalDate == end.atZone(zone).toLocalDate

  given Reads[BookingProtocol] =
    Json
      .reads[BookingProtocol]
      .filter(JsonValidationError("po", "invalid structure of po"))(p =>
        p.kind != BookingKind.Teaching || p.po.exists(validPO)
      )
}
