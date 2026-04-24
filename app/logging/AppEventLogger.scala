package logging

import java.util.UUID

import play.api.Logger

opaque type CorrelationId = UUID

object CorrelationId {
  def random(): CorrelationId = UUID.randomUUID()

  def fromUUID(value: UUID): CorrelationId = value

  extension (id: CorrelationId) {
    def value: UUID = id
  }
}

enum LogResult(val value: String) {
  case Started   extends LogResult("started")
  case Succeeded extends LogResult("succeeded")
  case Failed    extends LogResult("failed")
  case Skipped   extends LogResult("skipped")
}

final case class LogEvent(
    event: String,
    result: LogResult,
    correlationId: CorrelationId,
    moduleId: Option[UUID] = None,
    mrId: Option[Int] = None,
    branch: Option[String] = None,
    actor: Option[String] = None,
    errorCode: Option[String] = None,
    details: Map[String, String] = Map.empty
)

object AppEventLogger {
  def info(logger: Logger, event: LogEvent): Unit =
    logger.info(render(event))

  def warn(logger: Logger, event: LogEvent): Unit =
    logger.warn(render(event))

  def error(logger: Logger, event: LogEvent, throwable: Throwable): Unit =
    logger.error(render(event), throwable)

  def error(logger: Logger, event: LogEvent): Unit =
    logger.error(render(event))

  private def render(event: LogEvent): String = {
    val base = List(
      Some(s"event=${event.event}"),
      Some(s"result=${event.result.value}"),
      Some(s"correlationId=${event.correlationId}"),
      event.moduleId.map(id => s"moduleId=$id"),
      event.mrId.map(id => s"mrId=$id"),
      event.branch.map(value => s"branch=$value"),
      event.actor.map(value => s"actor=$value"),
      event.errorCode.map(value => s"errorCode=$value")
    ).flatten
    val detailParts = event.details.toSeq.sortBy(_._1).map((key, value) => s"$key=$value")
    (base ++ detailParts).mkString(" ")
  }
}
