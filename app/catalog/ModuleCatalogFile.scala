package catalog

import models.{Semester, StudyProgramView}
import printing.PrintingLanguage

import java.nio.file.Path

case class ModuleCatalogFile[PDF](
    filename: String,
    studyProgram: StudyProgramView,
    semester: Semester,
    content: String,
    texFile: Path,
    pdfFile: PDF,
    lang: PrintingLanguage
)