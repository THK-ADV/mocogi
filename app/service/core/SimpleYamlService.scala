package service.core

import database.repo.Repository
import parser.Parser

import scala.concurrent.Future

trait SimpleYamlService[A] extends YamlService[A] {
  def fileParser: Parser[List[A]]
  def repo: Repository[A, A, _]

  def createOrUpdateMany(xs: Seq[A]): Future[Seq[A]] =
    repo.createOrUpdateMany(xs)

  def all(): Future[Seq[A]] =
    repo.all()

  override def parser: Future[Parser[List[A]]] =
    Future.successful(fileParser)
}
