package service

import basedata.{Faculty, Person}
import database.repo.PersonRepository
import parsing.base.PersonFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

trait PersonService {
  def all(): Future[Seq[Person]]

  def create(input: String): Future[Seq[Person]]
}

@Singleton
final class PersonServiceImpl @Inject() (
    val repo: PersonRepository,
    val parser: PersonFileParser
) extends PersonService {

  implicit def faculties: Seq[Faculty] = Nil

  def all(): Future[Seq[Person]] =
    repo.all()

  def create(input: String): Future[Seq[Person]] =
    parser.fileParser.parse(input)._1.fold(Future.failed, repo.createMany)
}
