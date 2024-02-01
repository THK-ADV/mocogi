package service.core

import database.InsertOrUpdateResult
import database.repo.Repository
import ops.EitherOps.EThrowableOps
import parser.Parser

import scala.concurrent.{ExecutionContext, Future}

trait YamlService[Input, Output] {
  def repo: Repository[Input, Output, _]
  def parser: Future[Parser[List[Output]]]
  implicit def ctx: ExecutionContext

  def all(): Future[Seq[Output]] =
    repo.all()

  def toInput(output: Output): Input

  def create(input: String): Future[Seq[Input]] =
    for {
      parser <- parser
      res <- parser.parse(input)._1.toFuture
      res <- repo.createMany(res.map(toInput))
    } yield res

  def createOrUpdate(
      input: String
  ): Future[Seq[(InsertOrUpdateResult, Input)]] =
    for {
      parser <- parser
      res <- parser.parse(input)._1.toFuture
      res <- repo.createOrUpdateMany(res.map(toInput))
    } yield res
}
