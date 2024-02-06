package service.core

import database.repo.core.IdentityRepository
import models.core.Identity
import models.core.Identity.toDbEntry
import parsing.core.IdentityFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class IdentityService @Inject() (
    val repo: IdentityRepository,
    val facultyService: FacultyService,
    implicit val ctx: ExecutionContext
) extends YamlService[Identity] {

  override protected def parser =
    facultyService.all().map(IdentityFileParser.parser(_))

  override def createOrUpdateMany(
      xs: Seq[Identity]
  ): Future[Seq[Identity]] =
    repo.createOrUpdateMany(xs.map(toDbEntry)).map(_ => xs)

  override def all(): Future[Seq[Identity]] = repo.all()
}
