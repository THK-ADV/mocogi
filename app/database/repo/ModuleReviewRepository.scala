package database.repo

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.ModuleReviewRepository.PendingModuleReview
import database.table.core.DegreeTable
import database.table.core.IdentityTable
import database.table.core.StudyProgramPersonTable
import database.table.core.StudyProgramTable
import database.table.ModuleDraftTable
import database.table.ModuleReviewTable
import models.*
import models.core.IDLabel
import models.core.Identity
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleReviewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[ModuleReview.DB, ModuleReview.DB, ModuleReviewTable]
    with HasDatabaseConfigProvider[JdbcProfile] {

  import database.table.moduleReviewStatusColumnType
  import profile.api.*

  protected val tableQuery = TableQuery[ModuleReviewTable]

  def delete(moduleId: UUID): Future[Int] =
    db.run(tableQuery.filter(_.moduleDraft === moduleId).delete)

  def moduleId(ids: List[UUID]): Future[UUID] =
    db.run(tableQuery.filter(_.id.inSet(ids)).result).flatMap { reviews =>
      if reviews.isEmpty then Future.failed(new Exception(s"expected one module for review ids ($ids)"))
      else {
        val moduleId     = reviews.head.moduleDraft
        val isSameModule = reviews.forall(_.moduleDraft == moduleId)
        if isSameModule then Future.successful(moduleId)
        else Future.failed(new Exception(s"review ids ($ids) must belong to one module ($moduleId), but was: $reviews"))
      }
    }

  def getStatusByModule(moduleId: UUID): Future[Seq[ModuleReviewStatus]] =
    db.run(tableQuery.filter(_.moduleDraft === moduleId).map(_.status).result)

  def update(ids: List[UUID], status: ModuleReviewStatus, comment: Option[String], person: String): Future[Int] =
    db.run(
      tableQuery
        .filter(_.id.inSet(ids))
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
              respondedBy = p.collect { case p if p.isPerson => Identity.toPersonUnsafe(p) }
            )
        })
    )

  def getAllPending(): Future[Seq[PendingModuleReview]] = {
    import database.table.{ moduleReviewStatusColumnType, universityRoleColumnType }

    val base = tableQuery
      .filterNot(_.isApproved)
      .join(TableQuery[StudyProgramTable])
      .on(_.studyProgram === _.id)
      .join(TableQuery[DegreeTable])
      .on(_._2.degree === _.id)
      .map { case ((r, sp), d) => (r, sp.deLabel, d.deLabel) }

    val moduleDraftQuery =
      for
        d <- TableQuery[ModuleDraftTable]
        a <- d.authorFk if a.isPerson
      yield (d.module, d.moduleTitle, d.moduleAbbrev, a.id, a.firstname, a.lastname, a.campusId)

    val studyProgramDirQuery =
      for
        q <- TableQuery[StudyProgramPersonTable]
        d <- q.personFk if d.isPerson && d.isActive
      yield (q.studyProgram, q.role, d.id, d.firstname, d.lastname, d.campusId)

    val query = base
      .join(moduleDraftQuery)
      .on(_._1.moduleDraft === _._1)
      .join(studyProgramDirQuery)
      .on((a, b) => a._1._1.studyProgram === b._1 && a._1._1.role === b._2)
      .map { case (((r, sp, d), md), dir) => ((r.id, r.role, sp, d, r.status), md, dir) }
      .result
      .map(
        _.groupBy(_._2._1)
          .filterNot(_._2.exists(_._1._5.isRejected))
          .values
          .flatMap(_.map {
            case (r, md, dir) =>
              PendingModuleReview(
                r._1,
                r._2,
                r._3,
                r._4,
                ModuleCore(md._1, md._2, md._3),
                PersonCore(md._4, md._5, md._6, md._7),
                PersonCore(dir._3, dir._4, dir._5, dir._6)
              )
          })
          .toSeq
      )

    db.run(query)
  }

  protected override def retrieve(query: Query[ModuleReviewTable, ModuleReview.DB, Seq]): Future[Seq[ModuleReview.DB]] =
    db.run(query.result)
}

object ModuleReviewRepository {
  case class PendingModuleReview(
      reviewId: UUID,
      reviewRole: UniversityRole,
      reviewStudyProgramLabel: String,
      reviewDegreeLabel: String,
      module: ModuleCore,
      moduleAuthor: PersonCore,
      director: PersonCore
  )
}
