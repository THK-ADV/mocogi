package service

import basedata.Person.{Group, Single, Unknown}
import basedata.{Person, PersonStatus}
import database.InsertOrUpdateResult
import database.repo.PersonRepository
import database.table.PersonDbEntry
import parsing.base.PersonFileParser

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
  ): Future[List[(InsertOrUpdateResult, Person)]] =
    for {
      faculties <- facultyService.all()
      people <- parser.parser(faculties).parse(input)._1 match {
        case Left(e) =>
          Future.failed(e)
        case Right(xs) =>
          repo.createOrUpdateMany(xs.map(toDbEntry)).map(_.map(_._1).zip(xs))
      }
    } yield people

  private def toDbEntry(p: Person): PersonDbEntry =
    p match {
      case Single(
            id,
            lastname,
            firstname,
            title,
            faculties,
            abbreviation,
            status
          ) =>
        PersonDbEntry(
          id,
          lastname,
          firstname,
          title,
          faculties.map(_.abbrev),
          abbreviation,
          status,
          Person.SingleKind
        )
      case Group(id, title) =>
        PersonDbEntry(
          id,
          "",
          "",
          title,
          Nil,
          "",
          PersonStatus.Active,
          Person.GroupKind
        )
      case Unknown(id, title) =>
        PersonDbEntry(
          id,
          "",
          "",
          title,
          Nil,
          "",
          PersonStatus.Active,
          Person.UnknownKind
        )
    }

}
