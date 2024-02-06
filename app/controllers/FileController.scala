package controllers

import controllers.Assets.Asset
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import providers.ConfigReader

import java.nio.file.Paths
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object FileController {
  private def assetsPath = "files"
  def makeURI(folder: String, filename: String) =
    s"$assetsPath/$folder/$filename"
}

@Singleton
final class FileController @Inject() (
    cc: ControllerComponents,
    configReader: ConfigReader,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def get(asset: Asset) =
    Action { r =>
      if (!asset.name.split('/').lastOption.exists(_.split('.').length == 2))
        BadRequest(Json.obj("message" -> s"invalid file ${asset.name}"))
      else
        try {
          val path =
            Paths.get(s"${configReader.outputFolderPath}/${asset.name}")
          Ok.sendFile(content = path.toFile, fileName = f => Some(f.getName))
        } catch {
          case NonFatal(e) =>
            ErrorHandler.internalServerError(
              r.toString(),
              s"file not found: ${e.getMessage}",
              e.getStackTrace
            )
        }
    }
}
