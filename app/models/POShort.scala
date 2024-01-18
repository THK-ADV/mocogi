package models

import models.core.{Grade, Specialization}

case class POShort(
    abbrev: String,
    version: Int,
    specialization: Option[SpecializationShort],
    studyProgram: StudyProgramShort
) {
  def fullAbbrev = specialization.fold(abbrev)(_.abbrev)
}

object POShort {
  def apply(
      t: (String, Int, (String, String, String, Grade)),
      s: Option[Specialization]
  ): POShort =
    POShort(
      t._1,
      t._2,
      s.map(s => SpecializationShort(s.abbrev, s.label)),
      StudyProgramShort(t._3)
    )
}
