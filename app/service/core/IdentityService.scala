package service.core

import database.repo.IdentityRepository
import database.table.IdentityDbEntry
import models.core.Identity
import models.core.Identity.toDbEntry
import parsing.core.IdentityFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait IdentityService extends YamlService[IdentityDbEntry, Identity]

@Singleton
final class IdentityServiceImpl @Inject() (
    val repo: IdentityRepository,
    val facultyService: FacultyService,
    implicit val ctx: ExecutionContext
) extends IdentityService {

  override def toInput(output: Identity): IdentityDbEntry = toDbEntry(output)

  override def parser = facultyService.all().map(IdentityFileParser.parser(_))
}
