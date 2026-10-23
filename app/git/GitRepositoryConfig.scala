package git

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class GitRepositoryConfig(
    mainBranch: Branch,
    previewBranch: Branch,
    modulesPath: String,
    moduleCatalogsPath: String,
    moduleCompanionsPath: String
)

object GitRepositoryConfig {
  def load(configuration: Configuration): GitRepositoryConfig =
    GitRepositoryConfig(
      mainBranch = Branch(configuration.nonEmptyString("git.mainBranch")),
      previewBranch = Branch(configuration.nonEmptyString("git.draftBranch")),
      modulesPath = configuration.nonEmptyString("git.modulesFolder"),
      moduleCatalogsPath = configuration.nonEmptyString("git.moduleCatalogsFolder"),
      moduleCompanionsPath = configuration.nonEmptyString("git.moduleCompanionFolder")
    )
}
