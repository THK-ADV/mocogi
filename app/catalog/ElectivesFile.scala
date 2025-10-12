package catalog

import java.nio.file.Path

import catalog.ElectivesFile.fileExt
import models.Semester

case class ElectivesFile(path: Path) extends AnyVal {
  def hasFileName(semester: Semester): Boolean = {
    val fileName = this.fileName
    fileName.startsWith(semester.id) && fileName.endsWith(fileExt)
  }

  def fileName = path.getFileName.toString

  def teachingUnit: Option[String] =
    fileName.split("_").lastOption.map(_.stripSuffix(fileExt))
}

object ElectivesFile {

  private def fileExt = ".csv"

  def fileName(semester: Semester, teachingUnit: String): String =
    s"${semester.id}_$teachingUnit$fileExt"
}
