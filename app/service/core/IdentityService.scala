package service.core

import database.InsertOrUpdateResult
import database.repo.IdentityRepository
import models.core.Identity
import models.core.Identity.toDbEntry
import parsing.core.IdentityFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait IdentityService {
  def all(): Future[Seq[Identity]]
  def create(input: String): Future[Seq[Identity]]
  def createOrUpdate(
      input: String
  ): Future[Seq[(InsertOrUpdateResult, Identity)]]
}

@Singleton
final class IdentityServiceImpl @Inject() (
    val repo: IdentityRepository,
    val parser: IdentityFileParser,
    val facultyService: FacultyService,
    implicit val ctx: ExecutionContext
) extends IdentityService {

  def all(): Future[Seq[Identity]] =
    repo.all()

  def create(input: String): Future[List[Identity]] =
    for {
      faculties <- facultyService.all()
      identities <- parser.parser(faculties).parse(input)._1 match {
        case Left(e) =>
          Future.failed(e)
        case Right(xs) =>
          repo.createMany(xs.map(toDbEntry)).map(_ => xs)
      }
    } yield identities

  def createOrUpdate(
      input: String
  ): Future[Seq[(InsertOrUpdateResult, Identity)]] =
    for {
      faculties <- facultyService.all()
      identities <- parser.parser(faculties).parse(input)._1 match {
        case Left(e) =>
          Future.failed(e)
        case Right(xs) =>
          repo.createOrUpdateMany(xs.map(toDbEntry)).map(_.map(_._1).zip(xs))
      }
    } yield identities

}
