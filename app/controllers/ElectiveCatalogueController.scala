package controllers

import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

import scala.jdk.CollectionConverters.IteratorHasAsScala

import catalog.ElectivesFile
import models.core.IDLabel
import models.Semester
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import providers.ConfigReader

@deprecated("Discuss the existence of this class")
@Singleton
final class ElectiveCatalogueController @Inject() (
    cc: ControllerComponents,
    configReader: ConfigReader
) extends AbstractController(cc) {

  def allFromSemester(semesterId: String) =
    Action { (_: Request[AnyContent]) =>
      val semester = Semester(semesterId)
      val folder   = Paths.get(configReader.electivesCatalogOutputFolderPath)
      val json = Files
        .walk(folder)
        .iterator()
        .asScala
        .drop(1)
        .map(ElectivesFile.apply)
        .collect {
          case file if file.hasFileName(semester) =>
            val teachingUnit = file.teachingUnit match {
              case Some("inf") =>
                IDLabel("inf", "Informatik", "Computer Science")
              case Some("ing") =>
                IDLabel("ing", "Ingenieurwissenschaften", "Engineering")
              case _ => IDLabel("-", "-", "-")
            }
            Json.obj(
              "semester"     -> semester,
              "teachingUnit" -> teachingUnit,
              "url" -> FileController
                .makeURI(folder.getFileName.toString, file.fileName)
            )
        }
      Ok(JsArray.apply(json.toSeq))
    }
}
