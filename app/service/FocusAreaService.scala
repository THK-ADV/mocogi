package service

import basedata.FocusArea
import database.repo.FocusAreaRepository
import parsing.base.FocusAreaFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait FocusAreaService {
  def all(): Future[Seq[FocusArea]]
  def create(input: String): Future[List[FocusArea]]
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
}
