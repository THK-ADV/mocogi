package controllers.formats

import models.{ModuleDraft, ModuleDraftProtocol, ModuleDraftStatus}
import play.api.libs.json.{Format, JsValue, Json, Writes}
import service.Print

trait ModuleDraftFormat
    extends JsonNullWritable
    with ModuleCompendiumProtocolFormat {
  implicit val moduleDraftStatusFmt: Format[ModuleDraftStatus] =
    Format.of[String].bimap(ModuleDraftStatus.apply, _.toString)

  implicit val validationFmt: Writes[Either[JsValue, (JsValue, Print)]] =
    Writes.apply(
      {
        case Left(err) =>
          Json.obj(
            "tag" -> "error",
            "value" -> err
          )
        case Right((json, _)) =>
          Json.obj(
            "tag" -> "moduleCompendium",
            "value" -> json
          )
      }
    )

  implicit val moduleDraftFmt: Writes[ModuleDraft] =
    Json.writes[ModuleDraft]

  implicit val moduleDraftProtocolFmt: Format[ModuleDraftProtocol] =
    Json.format[ModuleDraftProtocol]
}
