package basedata

import java.time.LocalDate

case class PO(
    abbrev: String,
    version: Int,
    date: LocalDate,
    dateFrom: LocalDate,
    dateTo: Option[LocalDate],
    modificationDates: List[LocalDate],
    program: StudyProgramPreview
)
