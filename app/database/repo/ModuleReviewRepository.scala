package database.repo

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.core.IdentityTable
import database.table.core.StudyProgramTable
import database.table.ModuleReviewTable
import models.core.IDLabel
import models.core.Identity
import models.ModuleReview
import models.ModuleReviewStatus
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleReviewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
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
        .join(TableQuery[StudyProgramTable])
        .on(_.studyProgram === _.id)
        .joinLeft(TableQuery[IdentityTable])
        .on(_._1.respondedBy === _.id)
        .result
        .map(_.map {
          case ((r, sp), p) =>
            r.copy(
              studyProgram = IDLabel(sp.id, sp.deLabel, sp.enLabel),
              respondedBy = p.collect {
                case p if p.kind == Identity.PersonKind =>
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

  protected override def retrieve(
      query: Query[ModuleReviewTable, ModuleReview.DB, Seq]
  ): Future[Seq[ModuleReview.DB]] = db.run(query.result)
}
