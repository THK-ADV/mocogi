package webhook

import javax.inject.Inject

import cli.GitCLI
import git.Branch
import logging.AppEventLogger
import logging.CorrelationId
import logging.LogEvent
import logging.LogResult
import org.apache.pekko.actor.Actor
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.Logging

/**
 * This class keeps the local preview branch in sync with the remote branch.
 */
final class PreviewPushEventHandler @Inject() (cli: GitCLI) extends Actor with Logging {
  override def receive = {
    case HandleEvent(json, incomingCorrelationId) =>
      implicit val correlationId: CorrelationId = incomingCorrelationId
      val event                                 = "git.push.preview.process"
      infoEvent(event = event, result = LogResult.Started)
      parseBranch(json) match {
        case JsSuccess(branch, _) if branch.value == cli.draftBranch.value =>
          val exitCode = cli.updatePreviewBranch()
          if exitCode == 0 then
            infoEvent(
              event = event,
              result = LogResult.Succeeded,
              branch = Some(branch),
              details = Map("exitCode" -> exitCode.toString)
            )
          else
            errorEvent(
              event = event,
              result = LogResult.Failed,
              branch = Some(branch),
              errorCode = Some("preview_branch_update_failed"),
              details = Map("exitCode" -> exitCode.toString)
            )
        case JsSuccess(branch, _) =>
          infoEvent(
            event = event,
            result = LogResult.Skipped,
            branch = Some(branch),
            details = Map("reason" -> "not_preview_branch")
          )
        case JsError(errors) =>
          warnEvent(
            event = event,
            result = LogResult.Skipped,
            details = Map("reason" -> "invalid_event_payload")
          )
          logUnhandedEvent(logger, errors)
      }
  }

  private def infoEvent(
      event: String,
      result: LogResult,
      branch: Option[Branch] = None,
      details: Map[String, String] = Map.empty
  )(implicit correlationId: CorrelationId): Unit =
    AppEventLogger.info(
      logger,
      LogEvent(
        event = event,
        result = result,
        correlationId = correlationId,
        branch = branch.map(_.value),
        details = details
      )
    )

  private def warnEvent(
      event: String,
      result: LogResult,
      branch: Option[Branch] = None,
      errorCode: Option[String] = None,
      details: Map[String, String] = Map.empty
  )(implicit correlationId: CorrelationId): Unit =
    AppEventLogger.warn(
      logger,
      LogEvent(
        event = event,
        result = result,
        correlationId = correlationId,
        branch = branch.map(_.value),
        errorCode = errorCode,
        details = details
      )
    )

  private def errorEvent(
      event: String,
      result: LogResult,
      branch: Option[Branch] = None,
      errorCode: Option[String] = None,
      details: Map[String, String] = Map.empty
  )(implicit correlationId: CorrelationId): Unit =
    AppEventLogger.error(
      logger,
      LogEvent(
        event = event,
        result = result,
        correlationId = correlationId,
        branch = branch.map(_.value),
        errorCode = errorCode,
        details = details
      )
    )
}
