package service

import database.InsertOrUpdateResult
import database.repo.Repository
import parsing.base.FileParser

import scala.concurrent.Future

trait YamlService[Input, Output] {
  def repo: Repository[Input, Output, _]
  def parser: FileParser[Output]

  def all(): Future[Seq[Output]] =
    repo.all()

  def toInput(output: Output): Input

  def create(input: String): Future[List[Input]] =
    parser.fileParser
      .parse(input)
      ._1
      .fold(Future.failed, xs => repo.createMany(xs.map(toInput)))

  def createOrUpdate(
      input: String
  ): Future[List[(InsertOrUpdateResult, Input)]] =
    parser.fileParser
      .parse(input)
      ._1
      .fold(Future.failed, xs => repo.createOrUpdateMany(xs.map(toInput)))
}
