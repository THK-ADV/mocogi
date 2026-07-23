package database.repo.core

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.Repository
import database.table.core.AssessmentMethodDbEntry
import database.table.core.AssessmentMethodTable
import database.table.ModulePermittedAssessmentMethodTable
import database.table.ModuleUsedAssessmentMethodTable
import models.core.AssessmentMethod
import models.AssessmentMethodSource
import models.ModulePermittedAssessmentMethod
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class AssessmentMethodRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[AssessmentMethodDbEntry, AssessmentMethod, AssessmentMethodTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.given_BaseColumnType_AssessmentMethodSource
  import profile.api.*

  protected val tableQuery            = TableQuery[AssessmentMethodTable]
  private val permittedForModuleQuery = TableQuery[ModulePermittedAssessmentMethodTable]

  def allIds(): Future[Seq[String]] =
    db.run(tableQuery.map(_.id).result)

  def allRPO(): Future[Seq[AssessmentMethod]] =
    allBySource(AssessmentMethodSource.RPO)

  def deleteMany(ids: Seq[String]): Future[Int] =
    db.run(tableQuery.filter(_.id.inSet(ids)).delete)

  protected override def retrieve(
      query: Query[AssessmentMethodTable, AssessmentMethodDbEntry, Seq]
  ): Future[Seq[AssessmentMethod]] =
    db.run(query.result.map(_.map(a => AssessmentMethod(a.id, a.deLabel, a.enLabel))))

  /**
   * Counts how many distinct modules use each assessment method, restricted to
   * methods originating from the examination regulations (RPO). The resulting
   * map is keyed by assessment method id.
   */
  def moduleCountPerMethod(): Future[Map[String, Int]] = {
    val query = (for {
      used <- TableQuery[ModuleUsedAssessmentMethodTable]
      am   <- used.assessmentMethodFk if am.source === AssessmentMethodSource.RPO
    } yield (used.assessmentMethod, used.module))
      .groupBy(_._1)
      .map { case (method, grp) => method -> grp.map(_._2).countDistinct }
    db.run(query.result).map(_.toMap)
  }

  /**
   * Returns the subset of assessment methods a module is permitted to use. When
   * the module defines no explicit subset, falls back to the full set of
   * methods from the examination regulations (RPO).
   */
  def allPermittedForModuleOrDefault(module: UUID): Future[Seq[String]] =
    allPermittedForModule(module).flatMap { permitted =>
      if permitted.isEmpty then allRPO().map(_.map(_.id)) else Future.successful(permitted)
    }

  /**
   * Returns each module's permitted assessment methods. Modules without an explicit subset fall back to the RPO labels.
   */
  def allPermittedLabelsForModulesOrDefault(modules: Seq[UUID]): Future[Map[UUID, Seq[AssessmentMethod]]] = {
    val moduleIds = modules.toSet
    if moduleIds.isEmpty then Future.successful(Map.empty)
    else {
      val query = for {
        q <- permittedForModuleQuery if q.module.inSet(moduleIds)
        am = q.permittedMethodIdsUnnest()
        amQ <- tableQuery if amQ.id === am
      } yield (q.module, amQ)

      db.run(query.distinct.result).flatMap { rows =>
        val permittedByModule     = rows.groupMap(_._1)((_, m) => AssessmentMethod(m.id, m.deLabel, m.enLabel))
        val modulesWithoutPermits = moduleIds -- permittedByModule.keySet

        if modulesWithoutPermits.isEmpty then Future.successful(permittedByModule)
        else allRPO().map(methods => permittedByModule ++ modulesWithoutPermits.map(_ -> methods))
      }
    }
  }

  /**
   * Sets the permitted assessment methods for a module, replacing any existing
   * definition. An empty list deletes the module's definition entirely.
   *
   * Fails if any given id does not resolve to a known assessment method.
   */
  def replaceForModule(module: UUID, assessmentMethods: List[String]): Future[Unit] = {
    val ids    = assessmentMethods.toSet
    val action =
      if ids.isEmpty then permittedForModuleQuery.filter(_.module === module).delete.void
      else
        for {
          known <- tableQuery.filter(_.id.inSet(ids)).map(_.id).result
          unknown = ids -- known.toSet
          _ <-
            if unknown.isEmpty then
              permittedForModuleQuery.insertOrUpdate(ModulePermittedAssessmentMethod(module, ids.toList))
            else DBIO.failed(new NoSuchElementException(s"unknown assessment methods: ${unknown.mkString(", ")}"))
        } yield ()
    db.run(action)
  }

  /**
   * Returns the subset of assessment methods a module is permitted to use, as
   * defined for that module on top of the global catalog.
   */
  private def allPermittedForModule(module: UUID): Future[Seq[String]] = {
    val query = for {
      q <- permittedForModuleQuery if q.module === module
      am = q.permittedMethodIdsUnnest()
      amQ <- tableQuery if amQ.id === am
    } yield amQ.id
    db.run(query.distinct.result)
  }

  private def allBySource(source: AssessmentMethodSource): Future[Seq[AssessmentMethod]] =
    retrieve(tableQuery.filter(_.source === source))
}
