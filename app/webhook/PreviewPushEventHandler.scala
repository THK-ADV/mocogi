package webhook

import javax.inject.Inject

import git.cli.ModuleGitCLI
import git.Branch
import org.apache.pekko.actor.Actor
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.Logging

/**
 * This class keeps the local preview branch in sync with the remote branch.
 */
final class PreviewPushEventHandler @Inject() (cli: ModuleGitCLI) extends Actor with Logging {
  override def receive = {
    case HandleEvent(json) =>
      logger.info("start handling git push event on preview branch")
      parseBranch(json) match {
        case JsSuccess(branch, _) if branch.value == cli.draftBranch.value =>
          val exitCode = cli.updatePreviewBranch()
          if exitCode == 0 then logger.info(s"successfully updated local ${branch.value} branch")
          else logger.error(s"failed to update local ${branch.value} branch: exit code $exitCode")
        case JsSuccess(branch, _) =>
          logger.info(s"can't handle action on branch ${branch.value}")
        case JsError(errors) =>
          logUnhandedEvent(logger, errors)
      }
  }
}
