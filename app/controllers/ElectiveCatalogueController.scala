package controllers

import models.{ElectivesFile, Semester}
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

  def allFromSemester(semesterId: String) =
    Action { _ =>
      val semester = Semester(semesterId)
      val folder = Paths.get(configReader.electivesCatalogOutputFolderPath)
      val json = Files
        .walk(folder)
        .iterator()
        .asScala
        .drop(1)
        .map(ElectivesFile.apply)
        .collect {
          case file if file.hasFileName(semester) =>
            Json.obj(
              "semester" -> semester,
              "url" -> FileController
                .makeURI(folder.getFileName.toString, file.fileName)
            )
        }
      Ok(JsArray.apply(json.toSeq))
    }
}
