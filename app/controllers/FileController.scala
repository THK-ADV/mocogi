package controllers

import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

import controllers.Assets.Asset
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import providers.ConfigReader

@deprecated
object FileController {
  private def assetsPath = "files"
  def makeURI(folder: String, filename: String) =
    s"$assetsPath/$folder/$filename"
}

@deprecated
@Singleton
final class FileController @Inject() (
    cc: ControllerComponents,
    configReader: ConfigReader,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def get(asset: Asset) =
    Action { (r: Request[AnyContent]) =>
      try {
        val path =
          Paths.get(s"${configReader.outputFolderPath}/${asset.name}")
        Ok.sendFile(content = path.toFile, fileName = f => Some(f.getName))
      } catch {
        case NonFatal(e) =>
          ErrorHandler.badRequest(r.toString(), e)
      }
    }
}
