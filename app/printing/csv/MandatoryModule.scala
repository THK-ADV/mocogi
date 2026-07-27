package printing.csv

import java.util.UUID

import models.MetadataProtocol

case class MandatoryModule(
    id: UUID,
    metadata: MetadataProtocol,
    semesters: List[Int]
)
