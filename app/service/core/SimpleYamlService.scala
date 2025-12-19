package service.core

import scala.concurrent.Future

import database.repo.Repository
import parser.Parser

private[core] trait SimpleYamlService[A] extends YamlService[A] {
  def fileParser: Parser[List[A]]
  def repo: Repository[A, A, ?]

  def createOrUpdateMany(xs: Seq[A]): Future[Seq[A]] =
    repo.createOrUpdateMany(xs)

  def all(): Future[Seq[A]] =
    repo.all()

  override def parser: Future[Parser[List[A]]] =
    Future.successful(fileParser)
}
