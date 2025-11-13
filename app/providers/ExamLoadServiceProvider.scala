package providers

import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Provider

import scala.concurrent.ExecutionContext

import git.Branch
import ops.ConfigurationOps.Ops
import play.api.Configuration
import service.exam.ExamLoadService
import service.AssessmentMethodService

final class ExamLoadServiceProvider @Inject() (
    assessmentMethodService: AssessmentMethodService,
    ctx: ExecutionContext,
    config: Configuration,
) extends Provider[ExamLoadService] {
  override def get() = new ExamLoadService(
    assessmentMethodService,
    Branch(config.nonEmptyString("git.draftBranch")),
    Paths.get(config.nonEmptyString("git.localGitFolderPath")),
    ctx
  )
}
