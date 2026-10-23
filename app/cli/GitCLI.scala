package cli

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.util.Try

import git.Branch
import models.MetadataProtocol
import models.ModuleProtocol
import ops.bimap
import ops.FileOps.getFilesOfDirectory
import parser.ParsingError
import parsing.RawModuleParser
import play.api.Logging

/** Reads and synchronizes the local module repository's preview branch. */
@Singleton
final class GitCLI @Inject() (config: GitCliConfig) extends Logging {

  def previewBranch: Branch = config.previewBranch

  /**
   * Retrieves all modules from the preview branch of the Git repository.
   *
   * This method attempts to update the repository's preview branch to the latest
   * state and parses all `.md` files in the Git folder as module descriptions.
   *
   * @return A tuple where the first element is a vector of parsing errors, and
   *         the second element is a vector of successfully parsed and their last modified time.
   *         Fails if the preview branch cannot be updated or its files cannot be read.
   */
  def getAllModulesFromPreview(): Try[(Vector[(ParsingError, String)], Vector[(ModuleProtocol, LocalDate)])] =
    Try {
      if updatePreviewBranch() != 0 then
        throw new IllegalStateException(s"Could not update Git preview branch ${previewBranch.value}")

      config.modulesDir
        .getFilesOfDirectory(_.getFileName.toString.endsWith(".md")) { (f: Path) =>
          val lastModified = Files
            .getLastModifiedTime(f)
            .toInstant
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate
          val content = Files.readString(f)
          RawModuleParser.parser.parse(content)._1.bimap(_ -> f.getFileName.toString, _ -> lastModified)
        }
        .partitionMap(identity)
    }

  /**
   * Retrieves [[MetadataProtocol]] by parsing the module from the preview branch of the Git repository.
   */
  def getFromPreview(module: UUID): Try[MetadataProtocol] =
    for {
      fileContent <- Try(Files.readString(config.modulesDir.resolve(s"$module.md")))
      (_, module) <- RawModuleParser.metadataParser.parse(fileContent)._1.toTry
    } yield module

  /**
   * Updates the local preview branch of the Git repository to match the latest state of the remote branch.
   *
   * @return An integer exit code indicating the success or failure of the update operation.
   *         A value of 0 indicates success, while any other value represents an error.
   */
  def updatePreviewBranch(): Int = {
    val context = config.modulesDir.toFile

    val fetchLatestChanges =
      Process(Seq("git", "fetch", "origin", previewBranch.value), context)
    val switchToBranch =
      Process(Seq("git", "switch", previewBranch.value), context)
    val resetToRemote =
      Process(Seq("git", "reset", "--hard", s"origin/${previewBranch.value}"), context)
    val log = ProcessLogger(logger.debug(_))

    (fetchLatestChanges #&& switchToBranch #&& resetToRemote).!(log)
  }
}
