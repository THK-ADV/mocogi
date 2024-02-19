package git

import play.api.libs.json.Reads

case class Diff(diff: String, path: GitFilePath)

object Diff {
  implicit def reads: Reads[Diff] =
    js =>
      for {
        diff <- js.\("diff").validate[String]
        path <- js.\("old_path").validate[String]
      } yield Diff(diff, GitFilePath(path))
}
