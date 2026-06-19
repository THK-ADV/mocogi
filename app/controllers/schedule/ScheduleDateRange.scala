package controllers.schedule

import java.sql.Timestamp
import java.time.Instant

import models.Semester
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest

private[schedule] object ScheduleDateRange {

  private def parseDate(key: String, request: RequestHeader): Option[Timestamp] =
    request.getQueryString(key).map(value => Timestamp.from(Instant.ofEpochMilli(value.toLong)))

  def resolve(request: RequestHeader): Either[Result, (Timestamp, Timestamp)] =
    request.getQueryString("semester") match {
      case Some(semester) =>
        val (from, to) = Semester.dateRange(semester)
        Right((Timestamp.valueOf(from), Timestamp.valueOf(to)))
      case None =>
        (parseDate("from", request), parseDate("to", request)) match {
          case (Some(from), Some(to)) => Right((from, to))
          case (None, None)           =>
            Left(BadRequest("Either `semester` or both `from` and `to` query parameters must be provided."))
          case _ =>
            Left(BadRequest("Both `from` and `to` query parameters must be provided together."))
        }
    }
}
