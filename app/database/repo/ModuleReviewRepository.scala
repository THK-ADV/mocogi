package database.repo

import database.table.{IdentityTable, ModuleReviewTable}
import database.view.StudyProgramViewRepository
import models.core.Identity
import models.{ModuleReview, ModuleReviewStatus}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleReviewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val studyProgramViewRepo: StudyProgramViewRepository,
    implicit val ctx: ExecutionContext
) extends Repository[
      ModuleReview.DB,
      ModuleReview.DB,
      ModuleReviewTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {

  import database.table.moduleReviewStatusColumnType
  import profile.api._

  protected val tableQuery = TableQuery[ModuleReviewTable]

  private val studyProgramQuery = studyProgramViewRepo.tableQuery

  def delete(moduleId: UUID): Future[Int] =
    db.run(tableQuery.filter(_.moduleDraft === moduleId).delete)

  def get(id: UUID): Future[Option[ModuleReview.DB]] =
    db.run(tableQuery.filter(_.id === id).result.map(_.headOption))

  def getStatusByModule(moduleId: UUID): Future[Seq[ModuleReviewStatus]] =
    db.run(tableQuery.filter(_.moduleDraft === moduleId).map(_.status).result)

  def update(
      id: UUID,
      status: ModuleReviewStatus,
      comment: Option[String],
      person: String
  ): Future[Int] =
    db.run(
      tableQuery
        .filter(_.id === id)
        .map(a => (a.status, a.comment, a.respondedBy, a.respondedAt))
        .update((status, comment, Some(person), Some(LocalDateTime.now)))
    )

  def getAtomicByModule(moduleId: UUID): Future[Seq[ModuleReview.Atomic]] =
    db.run(
      tableQuery
        .filter(_.moduleDraft === moduleId)
        .join(studyProgramQuery)
        .on(_.studyProgram === _.studyProgramId)
        .joinLeft(TableQuery[IdentityTable])
        .on(_._1.respondedBy === _.id)
        .result
        .map(_.map { case ((r, sp), p) =>
          r.copy(
            studyProgram = sp,
            respondedBy = p.collect {
              case p if p.kind == Identity.DefaultKind =>
                Identity.Person(
                  p.id,
                  p.lastname,
                  p.firstname,
                  p.title,
                  Nil,
                  p.abbreviation,
                  p.campusId.get,
                  p.status
                )
            }
          )
        })
    )

  override protected def retrieve(
      query: Query[ModuleReviewTable, ModuleReview.DB, Seq]
  ): Future[Seq[ModuleReview.DB]] = db.run(query.result)
}
