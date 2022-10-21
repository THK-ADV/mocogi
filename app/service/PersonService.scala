package service

import basedata.{Faculty, Person}
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
    implicit val ctx: ExecutionContext
) extends PersonService {

  implicit def faculties: Seq[Faculty] = Nil

  def all(): Future[Seq[Person]] =
    repo.all()

  def create(input: String): Future[List[Person]] =
    parser.fileParser
      .parse(input)
      ._1 match {
      case Left(e) => Future.failed(e)
      case Right(xs) =>
        repo.createMany(xs.map(makePersonDbEntry)).map(_ => xs)
    }
}
