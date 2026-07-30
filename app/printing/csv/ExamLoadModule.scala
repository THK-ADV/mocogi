package printing.csv

import java.util.UUID

import models.MetadataProtocol

case class ExamLoadModule(
    id: UUID,
    metadata: MetadataProtocol,
    semesters: List[Int]
)

case class ElectiveGroup(
    genericTitle: String,
    modules: Vector[ExamLoadModule]
)
