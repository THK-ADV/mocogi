package catalog

import java.nio.file.Path

import models.StudyProgramView
import printing.PrintingLanguage

case class ModuleCatalogFile[PDF](
    filename: String,
    studyProgram: StudyProgramView,
    semester: Semester,
    content: String,
    texFile: Path,
    pdfFile: PDF,
    lang: PrintingLanguage
)
