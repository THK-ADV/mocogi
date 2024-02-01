package models

import models.core.{Degree, Specialization}

// TODO remove?
case class POShort(
    id: String,
    version: Int,
    specialization: Option[SpecializationShort],
    studyProgram: StudyProgramShort
) {
  def fullId = specialization.fold(id)(_.id)
}

object POShort {
  def apply(
      t: (String, Int, (String, String, String, Degree)),
      s: Option[Specialization]
  ): POShort =
    POShort(
      t._1,
      t._2,
      s.map(s => SpecializationShort(s.id, s.label)),
      StudyProgramShort(t._3)
    )
}
