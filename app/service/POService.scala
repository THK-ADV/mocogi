package service

import basedata.PO
import database.repo.PORepository
import parsing.base.POFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait POService {
  def all(): Future[Seq[PO]]
  def create(input: String): Future[List[PO]]
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
}
