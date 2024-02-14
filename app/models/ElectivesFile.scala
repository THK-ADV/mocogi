package models

import models.ElectivesFile.fileExt

import java.nio.file.Path

case class ElectivesFile(path: Path) extends AnyVal {
  def hasFileName(semester: Semester): Boolean = {
    val fileName = this.fileName
    fileName.startsWith(semester.id) && fileName.endsWith(fileExt)
  }

  def fileName = path.getFileName.toString
}

object ElectivesFile {

  private def fileExt = ".csv"

  def fileName(semester: Semester): String =
    s"${semester.id}$fileExt"
}
