package controllers.formats

import models.{ModuleDraft, ModuleDraftProtocol}
import play.api.libs.json.{Format, Json, Writes}

trait ModuleDraftFormat
    extends JsonNullWritable
    with ModuleCompendiumProtocolFormat {

  implicit val moduleDraftFmt: Writes[ModuleDraft] =
    Writes.apply(d =>
      Json.obj(
        "module" -> d.module,
        "user" -> d.user.username,
        "status" -> d.status.toString,
        "data" -> d.data,
        "keysToBeReviewed" -> d.keysToBeReviewed,
        "mergeRequestAuthor" -> d.mergeRequest.map(_._2.username),
        "lastModified" -> d.lastModified
      )
    )

  implicit val moduleDraftProtocolFmt: Format[ModuleDraftProtocol] =
    Json.format[ModuleDraftProtocol]
}
