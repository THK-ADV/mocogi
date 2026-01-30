package models.schedule

import java.time.LocalDate
import java.util.UUID

case class SemesterPlan(
    id: UUID,
    start: LocalDate,
    end: LocalDate,
    kind: SemesterPlanType,
    teachingUnit: Option[UUID],
    semesterIndex: Option[List[Int]],
    phase: Option[String],
)
