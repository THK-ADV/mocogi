package git

import play.api.libs.json.Reads

/**
 * A changed file of a merge request. `path` is the new path, so a detected rename (git pairs a deleted and a similar
 * added file) points to the existing file. For deleted files, GitLab reports the same old and new path.
 */
case class Diff(diff: String, path: GitFilePath)

object Diff {
  implicit def reads: Reads[Diff] =
    js =>
      for {
        diff <- js.\("diff").validate[String]
        path <- js.\("new_path").validate[String]
      } yield Diff(diff, GitFilePath(path))
}
