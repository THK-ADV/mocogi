package service

import database.repo.Repository
import parsing.base.FileParser

import scala.concurrent.Future

trait YamlService[A] {
  def repo: Repository[A, _]
  def parser: FileParser[A]

  def all(): Future[Seq[A]] =
    repo.all()

  def create(input: String): Future[Seq[A]] =
    parser.fileParser.parse(input)._1.fold(Future.failed, repo.createMany)
}
