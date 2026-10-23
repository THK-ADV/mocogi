package cli

import java.nio.file.Path

import git.Branch
import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class GitCliConfig(previewBranch: Branch, modulesDir: Path)

object GitCliConfig {
  def load(configuration: Configuration): GitCliConfig =
    GitCliConfig(
      previewBranch = Branch(configuration.nonEmptyString("git.draftBranch")),
      modulesDir = Path.of(configuration.nonEmptyString("git.localGitFolderPath"))
    )
}
