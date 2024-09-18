package controllers

import catalog.{ElectivesFile, Semester}
import models.core.IDLabel
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{
  AbstractController,
  AnyContent,
  ControllerComponents,
  Request
}
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
    Action { (_: Request[AnyContent]) =>
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
            val teachingUnit = file.teachingUnit match {
              case Some("inf") =>
                IDLabel("inf", "Informatik", "Computer Science")
              case Some("ing") =>
                IDLabel("ing", "Ingenieurwissenschaften", "Engineering")
              case _ => IDLabel("-", "-", "-")
            }
            Json.obj(
              "semester" -> semester,
              "teachingUnit" -> teachingUnit,
              "url" -> FileController
                .makeURI(folder.getFileName.toString, file.fileName)
            )
        }
      Ok(JsArray.apply(json.toSeq))
    }
}
