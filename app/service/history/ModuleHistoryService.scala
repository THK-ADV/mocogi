package service.history

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.api.FileVersion
import git.api.GitCommitService
import git.GitConfig
import git.GitFilePath
import parsing.RawModuleParser
import models.ModuleCore
import models.Semester

@Singleton
final class ModuleHistoryService @Inject() (
    private val gitCommitService: GitCommitService,
    private implicit val config: GitConfig,
    private implicit val ctx: ExecutionContext
) {

  private def getSemester(committedAt: LocalDateTime): Semester =
    Semester.of(committedAt.toLocalDate)

  /**
   * Maps each FileVersion to a ModuleVersion, classifying the content as Parsed,
   * Deleted (no content) or ParseError (content present but invalid).
   */
  private def parseModules(versions: List[FileVersion]): List[ModuleVersion] = {
    val parser = RawModuleParser.parser
    versions.map {
      case FileVersion(commitId, committedAt, content) =>
        val parsed: ModuleVersionContent = content match {
          case None    => ModuleVersionContent.Deleted
          case Some(c) =>
            parser.parse(c.value)._1 match {
              case Right(m)  => ModuleVersionContent.Parsed(ModuleCore(m.id.get, m.metadata.title, m.metadata.abbrev), c)
              case Left(err) => ModuleVersionContent.ParseError(err.getMessage)
            }
        }
        ModuleVersion(commitId, committedAt, parsed, getSemester(committedAt))
    }
  }

  /**
   * Returns the full history of a module on the main branch since the configured cut-off date,
   *  with each version's content parsed into a ModuleProtocol.
   */
  def getModuleHistory(module: UUID): Future[List[ModuleVersion]] = {
    val filePath = GitFilePath(module)
    gitCommitService
      .getAllVersionsOfFile(filePath, config.mainBranch, config.historySince)
      .map(parseModules)
  }
}
