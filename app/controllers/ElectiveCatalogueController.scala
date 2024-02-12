package controllers

import models.Semester
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{AbstractController, ControllerComponents}
import providers.ConfigReader

import java.nio.file.{Files, Paths}
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.IteratorHasAsScala

@Singleton
final class ElectiveCatalogueController @Inject() (
    cc: ControllerComponents,
    configReader: ConfigReader
) extends AbstractController(cc) {

  def all() =
    Action { _ =>
      val folder = Paths.get(configReader.electivesCatalogOutputFolderPath)
      val json = Files
        .walk(folder)
        .iterator()
        .asScala
        .drop(1)
        .map { p =>
          val filenameWithExt = p.getFileName.toString
          val filenameOnly = filenameWithExt.split('.').head
          Json.obj(
            "semester" -> Semester(filenameOnly),
            "url" -> FileController
              .makeURI(folder.getFileName.toString, filenameWithExt)
          )
        }
      Ok(JsArray.apply(json.toSeq))
    }
}
