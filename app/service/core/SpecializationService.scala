package service.core

import database.InsertOrUpdateResult
import database.repo.SpecializationRepository
import models.core.Specialization
import parsing.core.SpecializationFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait SpecializationService {
  def all(): Future[Seq[Specialization]]
  def create(input: String): Future[List[Specialization]]
  def createOrUpdate(
      input: String
  ): Future[List[(InsertOrUpdateResult, Specialization)]]
}

@Singleton
final class SpecializationServiceImpl @Inject() (
    val repo: SpecializationRepository,
    val poService: POService,
    implicit val ctx: ExecutionContext
) extends SpecializationService {

  override def all(): Future[Seq[Specialization]] =
    repo.all()

  override def create(input: String): Future[List[Specialization]] =
    for {
      pos <- poService.allIds()
      specs <- SpecializationFileParser
        .fileParser(pos)
        .parse(input)
        ._1
        .fold(Future.failed, xs => repo.createMany(xs).map(_ => xs))
    } yield specs

  override def createOrUpdate(
      input: String
  ): Future[List[(InsertOrUpdateResult, Specialization)]] = {
    def go(xs: List[Specialization]) =
      Future.sequence(
        xs.map(x =>
          repo.exists(x.id).flatMap {
            case true  => repo.update(x).map(InsertOrUpdateResult.Update -> _)
            case false => repo.create(x).map(InsertOrUpdateResult.Insert -> _)
          }
        )
      )

    for {
      pos <- poService.allIds()
      specs <- SpecializationFileParser
        .fileParser(pos)
        .parse(input)
        ._1
        .fold(Future.failed, go)
    } yield specs
  }
}
