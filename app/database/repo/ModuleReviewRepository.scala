package database.repo

import database.table.{ModuleReviewTable, PersonTable, StudyProgramTable}
import models.core.Person
import models.{ModuleReview, ModuleReviewStatus, StudyProgramShort}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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

  def deleteMany(moduleIds: Seq[UUID]): Future[Int] =
    db.run(tableQuery.filter(_.moduleDraft.inSet(moduleIds)).delete)

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

  def getAtomicByModule(moduleId: UUID): Future[Seq[ModuleReview.Atomic]] = {
    val spQuery = for {
      sp <- TableQuery[StudyProgramTable]
      g <- sp.gradeFk
    } yield (sp.abbrev, sp.deLabel, sp.enLabel, g)

    db.run(
      tableQuery
        .filter(_.moduleDraft === moduleId)
        .join(spQuery)
        .on(_.studyProgram === _._1)
        .joinLeft(TableQuery[PersonTable])
        .on(_._1.respondedBy === _.id)
        .result
        .map(_.map { case ((r, sp), p) =>
          r.copy(
            studyProgram = StudyProgramShort(sp),
            respondedBy = p.collect {
              case p if p.kind == Person.DefaultKind =>
                Person.Default(
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
  }

  override protected def retrieve(
      query: Query[ModuleReviewTable, ModuleReview.DB, Seq]
  ): Future[Seq[ModuleReview.DB]] = db.run(query.result)
}
