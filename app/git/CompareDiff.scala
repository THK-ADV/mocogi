package git

import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.JsonNaming
import play.api.libs.json.OWrites
import play.api.libs.json.Reads

/** A file change between two revisions with its unified diff hunks. */
final case class CompareDiff(
    oldPath: String,
    newPath: String,
    newFile: Boolean,
    renamedFile: Boolean,
    deletedFile: Boolean,
    diff: String
)

object CompareDiff {
  given Reads[CompareDiff]   = Json.configured(JsonConfiguration(JsonNaming.SnakeCase)).reads
  given OWrites[CompareDiff] = Json.writes
}
