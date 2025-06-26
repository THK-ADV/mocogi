package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.IdentityRepository
import models.core.Identity
import models.core.Identity.toDbEntry
import parsing.core.IdentityFileParser

@Singleton
final class IdentityService @Inject() (
    val repo: IdentityRepository,
    implicit val ctx: ExecutionContext
) extends YamlService[Identity] {

  override def parser =
    Future.successful(IdentityFileParser.parser())

  override def createOrUpdateMany(xs: Seq[Identity]): Future[Seq[Identity]] =
    repo.createOrUpdateMany(xs.map(toDbEntry)).map(_ => xs)

  override def all(): Future[Seq[Identity]] =
    repo.all().map(_.map(Identity.fromDbEntry))

  def allByIds(ids: List[String]) =
    repo.allByIds(ids)
}
