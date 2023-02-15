package service.core

import database.InsertOrUpdateResult
import database.repo.PORepository
import models.core.PO
import parsing.core.POFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait POService {
  def all(): Future[Seq[PO]]
  def create(input: String): Future[List[PO]]
  def createOrUpdate(input: String): Future[List[(InsertOrUpdateResult, PO)]]
  def allValid(): Future[Seq[PO]]
}

@Singleton
final class POServiceImpl @Inject() (
    val repo: PORepository,
    val studyProgramService: StudyProgramService,
    implicit val ctx: ExecutionContext
) extends POService {

  override def all() =
    repo.all()

  override def create(input: String) =
    for {
      sps <- studyProgramService.allIds()
      pos <- POFileParser
        .fileParser(sps)
        .parse(input)
        ._1
        .fold(Future.failed, xs => repo.createMany(xs).map(_ => xs))
    } yield pos

  override def createOrUpdate(
      input: String
  ): Future[List[(InsertOrUpdateResult, PO)]] = {
    def go(xs: List[PO]) =
      Future.sequence(
        xs.map(po =>
          repo.exists(po.abbrev).flatMap {
            case true  => repo.update(po).map(InsertOrUpdateResult.Update -> _)
            case false => repo.create(po).map(InsertOrUpdateResult.Insert -> _)
          }
        )
      )

    for {
      sps <- studyProgramService.allIds()
      pos <- POFileParser
        .fileParser(sps)
        .parse(input)
        ._1
        .fold(Future.failed, go)
    } yield pos
  }

  override def allValid() =
    repo.allValid()
}
