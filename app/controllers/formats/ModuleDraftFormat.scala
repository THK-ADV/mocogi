package controllers.formats

import models.{ModuleDraft, ModuleDraftProtocol, ModuleDraftStatus}
import play.api.libs.json.{Format, Json}

trait ModuleDraftFormat {
  implicit val moduleDraftStatusFmt: Format[ModuleDraftStatus] =
    Format.of[String].bimap(ModuleDraftStatus.apply, _.toString)

  implicit val moduleDraftFmt: Format[ModuleDraft] =
    Json.format[ModuleDraft]

  implicit val moduleDraftProtocolFmt: Format[ModuleDraftProtocol] =
    Json.format[ModuleDraftProtocol]
}
