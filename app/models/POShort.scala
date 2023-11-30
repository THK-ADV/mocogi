package models

case class POShort(
    abbrev: String,
    version: Int,
    specialization: Option[SpecializationShort],
    studyProgram: StudyProgramShort
) {
  def fullAbbrev = specialization.fold(abbrev)(_.abbrev)
}
