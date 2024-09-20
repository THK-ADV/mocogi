package service.core

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import ops.EitherOps.EThrowableOps
import parser.Parser

trait YamlService[A] {
  def parser: Future[Parser[List[A]]]
  implicit def ctx: ExecutionContext

  def createOrUpdateMany(xs: Seq[A]): Future[Seq[A]]

  def all(): Future[Seq[A]]

  def createOrUpdate(
      input: String
  ): Future[Seq[A]] =
    for {
      parser <- parser
      res    <- parser.parse(input)._1.toFuture
      res    <- createOrUpdateMany(res)
    } yield res
}
