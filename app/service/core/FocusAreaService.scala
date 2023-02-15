package service.core

import database.InsertOrUpdateResult
import database.repo.FocusAreaRepository
import models.core.FocusArea
import parsing.core.FocusAreaFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait FocusAreaService {
  def all(): Future[Seq[FocusArea]]
  def create(input: String): Future[List[FocusArea]]
  def createOrUpdate(
      input: String
  ): Future[List[(InsertOrUpdateResult, FocusArea)]]
}

@Singleton
final class FocusAreaServiceImpl @Inject() (
    val repo: FocusAreaRepository,
    val studyProgramService: StudyProgramService,
    implicit val ctx: ExecutionContext
) extends FocusAreaService {

  override def all() =
    repo.all()

  override def create(input: String) =
    for {
      sps <- studyProgramService.allIds()
      pos <- FocusAreaFileParser
        .fileParser(sps)
        .parse(input)
        ._1
        .fold(Future.failed, xs => repo.createMany(xs).map(_ => xs))
    } yield pos

  def createOrUpdate(
      input: String
  ): Future[List[(InsertOrUpdateResult, FocusArea)]] =
    for {
      sps <- studyProgramService.allIds()
      pos <- FocusAreaFileParser
        .fileParser(sps)
        .parse(input)
        ._1
        .fold(
          Future.failed,
          xs => repo.createOrUpdateMany(xs).map(_.map(_._1).zip(xs))
        )
    } yield pos
}
