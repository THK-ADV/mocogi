package models.schedule

import java.util.UUID

import controllers.json.JsonNullWritable
import play.api.libs.json.Format
import play.api.libs.json.Json

/**
 * `examCapacityOpt` and `examCapacityMax` are the optimal and maximal seats under exam conditions, `seating` is one of
 * `tiered`, `fixed`, `loose` or `none` and `pcs` the number of PCs, each `None` if unknown. `equipment` lists planning
 * relevant equipment such as `projector` or `sound_system`, see evolution 19.
 */
case class Room(
    id: UUID,
    label: String,
    abbrev: String,
    `type`: String,
    capacity: Int,
    examCapacityOpt: Option[Int],
    examCapacityMax: Option[Int],
    seating: Option[String],
    pcs: Option[Int],
    equipment: List[String]
)

object Room extends JsonNullWritable {
  given Format[Room] = Json.format
}
