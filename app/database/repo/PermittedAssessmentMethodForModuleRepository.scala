package database.repo

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.core.AssessmentMethodTable
import database.table.PermittedAssessmentMethodForModuleTable
import models.core.AssessmentMethod
import models.PermittedAssessmentMethodForModule
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class PermittedAssessmentMethodForModuleRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api.*

  protected val tableQuery = TableQuery[PermittedAssessmentMethodForModuleTable]

  def all(): Future[Seq[PermittedAssessmentMethodForModule]] =
    db.run(tableQuery.result)

  def allByModule(module: UUID): Future[Seq[AssessmentMethod]] = {
    val query = for {
      q <- tableQuery if q.module === module
      am = q.assessmentMethodsUnnest()
      amQ <- TableQuery[AssessmentMethodTable] if amQ.id === am
    } yield (amQ.id, amQ.deLabel, amQ.enLabel)
    db.run(query.result.map(_.map(a => AssessmentMethod(a._1, a._2, a._3))))
  }

  def insert(xs: List[PermittedAssessmentMethodForModule]): Future[Option[Int]] =
    db.run(tableQuery ++= xs)
}
