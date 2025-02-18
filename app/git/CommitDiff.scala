package git

import play.api.libs.json.Reads

case class CommitDiff(newPath: GitFilePath, isNewFile: Boolean)

object CommitDiff {
  given Reads[CommitDiff] = js =>
    for
      newPath   <- js.\("new_path").validate[String]
      isNewFile <- js.\("new_file").validate[Boolean]
    yield CommitDiff(GitFilePath(newPath), isNewFile)
}
