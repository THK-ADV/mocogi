package service.core

import database.InsertOrUpdateResult
import database.repo.PersonRepository
import models.core.Person
import models.core.Person.toDbEntry
import parsing.core.PersonFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait PersonService {
  def all(): Future[Seq[Person]]
  def create(input: String): Future[Seq[Person]]
  def createOrUpdate(input: String): Future[Seq[(InsertOrUpdateResult, Person)]]
}

@Singleton
final class PersonServiceImpl @Inject() (
    val repo: PersonRepository,
    val parser: PersonFileParser,
    val facultyService: FacultyService,
    implicit val ctx: ExecutionContext
) extends PersonService {

  def all(): Future[Seq[Person]] =
    repo.all()

  def create(input: String): Future[List[Person]] =
    for {
      faculties <- facultyService.all()
      people <- parser.parser(faculties).parse(input)._1 match {
        case Left(e) =>
          Future.failed(e)
        case Right(xs) =>
          repo.createMany(xs.map(toDbEntry)).map(_ => xs)
      }
    } yield people

  def createOrUpdate(
      input: String
  ): Future[Seq[(InsertOrUpdateResult, Person)]] =
    for {
      faculties <- facultyService.all()
      people <- parser.parser(faculties).parse(input)._1 match {
        case Left(e) =>
          Future.failed(e)
        case Right(xs) =>
          repo.createOrUpdateMany(xs.map(toDbEntry)).map(_.map(_._1).zip(xs))
      }
    } yield people

}
