package database.table

import models.{ModuleDraft, ModuleDraftStatus}
import play.api.libs.json.JsValue
import service.Print
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime
import java.util.UUID

final class ModuleDraftTable(tag: Tag)
    extends Table[ModuleDraft](tag, "module_draft") {
  def module = column[UUID]("module_id", O.PrimaryKey)

  def data = column[String]("module_json")

  def branch = column[String]("branch")

  def status = column[ModuleDraftStatus]("status")

  def lastModified = column[LocalDateTime]("last_modified")

  def moduleCompendiumJson =
    column[Option[JsValue]]("valid_module_compendium_json")

  def moduleCompendiumPrint = column[Option[Print]]("module_compendium_print")

  def pipelineErrorJson = column[Option[JsValue]]("pipeline_error")

  override def * =
    (
      module,
      data,
      branch,
      status,
      lastModified,
      moduleCompendiumJson,
      moduleCompendiumPrint,
      pipelineErrorJson
    ) <> (mapRow, unmapRow)

  private def toValidation(
      moduleCompendiumJson: Option[JsValue],
      moduleCompendiumPrint: Option[Print],
      pipelineError: Option[JsValue]
  ): Option[Either[JsValue, (JsValue, Print)]] = {
    (moduleCompendiumJson, moduleCompendiumPrint, pipelineError) match {
      case (Some(json), Some(print), None) => Some(Right((json, print)))
      case (None, None, Some(err))         => Some(Left(err))
      case (None, None, None)              => None
      case (a, b, c) =>
        throw new Throwable(
          s"invalid database state for 'module_draft' table. expected 'validation' to be either success or failure, but was: $a, $b, $c"
        )
    }
  }

  private def fromValidation(
      e: Option[Either[JsValue, (JsValue, Print)]]
  ): (Option[JsValue], Option[Print], Option[JsValue]) =
    e match {
      case Some(e) =>
        e match {
          case Left(err)            => (None, None, Some(err))
          case Right((json, print)) => (Some(json), Some(print), None)
        }
      case None => (None, None, None)
    }
  private def mapRow: (
      (
          UUID,
          String,
          String,
          ModuleDraftStatus,
          LocalDateTime,
          Option[JsValue],
          Option[Print],
          Option[JsValue]
      )
  ) => ModuleDraft = {
    case (
          module,
          data,
          branch,
          status,
          lastModified,
          json,
          print,
          err
        ) =>
      ModuleDraft(
        module,
        data,
        branch,
        status,
        lastModified,
        toValidation(json, print, err)
      )
  }

  private def unmapRow: ModuleDraft => Option[
    (
        UUID,
        String,
        String,
        ModuleDraftStatus,
        LocalDateTime,
        Option[JsValue],
        Option[Print],
        Option[JsValue]
    )
  ] = { a =>
    val (json, print, err) = fromValidation(a.validation)
    Option(
      (
        a.module,
        a.data,
        a.branch,
        a.status,
        a.lastModified,
        json,
        print,
        err
      )
    )
  }
}
