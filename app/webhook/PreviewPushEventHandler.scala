package webhook

import javax.inject.Inject

import cli.GitCLI
import logging.errorC
import logging.infoC
import logging.warnC
import logging.CorrelationId
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
      given CorrelationId = incomingCorrelationId
      parseBranch(json) match {
        case JsSuccess(branch, _) if branch.value == cli.draftBranch.value =>
          val exitCode = cli.updatePreviewBranch()
          if exitCode == 0 then logger.infoC(s"preview branch updated branch=${branch.value}")
          else logger.errorC(s"preview branch update failed branch=${branch.value} exitCode=$exitCode")
        case JsSuccess(branch, _) =>
          logger.infoC(s"preview push skipped branch=${branch.value} reason=not_preview_branch")
        case JsError(errors) =>
          logger.warnC("preview push skipped reason=invalid_event_payload")
          logUnhandedEvent(logger, errors)
      }
  }
}
