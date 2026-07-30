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

extension (logger: Logger) {
  def infoC(msg: String)(using CorrelationId): Unit =
    logger.info(s"[${summon[CorrelationId]}] $msg")

  def warnC(msg: String)(using CorrelationId): Unit =
    logger.warn(s"[${summon[CorrelationId]}] $msg")

  def errorC(msg: String)(using CorrelationId): Unit =
    logger.error(s"[${summon[CorrelationId]}] $msg")

  def errorC(msg: String, t: Throwable)(using CorrelationId): Unit =
    logger.error(s"[${summon[CorrelationId]}] $msg", t)
}
