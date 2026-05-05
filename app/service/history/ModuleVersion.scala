package service.history

import java.time.LocalDateTime

import git.CommitId
import models.ModuleCore
import models.Semester
import play.api.libs.json.Json
import play.api.libs.json.Writes
import git.GitFileContent

/**
 * A single historical revision of a module, derived from a git commit.
 *
 * The `semester` indicates the academic term during which this revision was active,
 * allowing multiple revisions to be grouped under the same semester when rendering
 * a module's change history.
 */
final case class ModuleVersion(
    commitId: CommitId,
    committedAt: LocalDateTime,
    content: ModuleVersionContent,
    semester: Semester
)

object ModuleVersion {
  given Writes[ModuleVersion] = Json.writes[ModuleVersion]
}

enum ModuleVersionContent {
  case Parsed(module: ModuleCore, fileContent: GitFileContent)
  case Deleted
  case ParseError(message: String)
}

object ModuleVersionContent {
  given Writes[ModuleVersionContent] = Writes {
    case Parsed(m, c)    => Json.obj("type" -> "parsed", "module" -> Json.toJson(m), "fileContent" -> c.value)
    case Deleted         => Json.obj("type" -> "deleted")
    case ParseError(msg) => Json.obj("type" -> "parseError", "message" -> msg)
  }
}
