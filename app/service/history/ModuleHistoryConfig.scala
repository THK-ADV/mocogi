package service.history

import java.time.format.DateTimeParseException
import java.time.LocalDate

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class ModuleHistoryConfig(historySince: LocalDate)

object ModuleHistoryConfig {
  def load(configuration: Configuration): ModuleHistoryConfig = {
    val raw = configuration.nonEmptyString("git.historySince")
    try ModuleHistoryConfig(LocalDate.parse(raw))
    catch {
      case error: DateTimeParseException =>
        throw IllegalArgumentException("git.historySince must be an ISO-8601 date (YYYY-MM-DD)", error)
    }
  }
}
