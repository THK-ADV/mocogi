package printing.csv

import java.util.UUID

import models.MetadataProtocol

case class Module(id: UUID, metadata: MetadataProtocol, semester: List[Int])
