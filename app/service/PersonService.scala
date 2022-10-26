package service

import basedata.Person
import database.repo.PersonRepository
import parsing.base.PersonFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait PersonService {
  def all(): Future[Seq[Person]]
  def create(input: String): Future[Seq[Person]]
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
          repo.createMany(xs.map(makePersonDbEntry)).map(_ => xs)
      }
    } yield people
}
