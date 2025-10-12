package catalog

import java.nio.file.Path
import models.{Semester, StudyProgramView}

case class ModuleCatalogFile[PDF](
    filename: String,
    studyProgram: StudyProgramView,
    semester: Semester,
    content: String,
    texFile: Path,
    pdfFile: PDF
)
