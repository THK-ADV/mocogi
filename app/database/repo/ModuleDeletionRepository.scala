package database.repo

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.CreatedModuleTable
import database.table.ModuleCompanionTable
import database.table.ModuleDraftTable
import database.table.ModuleReviewTable
import database.table.ModuleUpdatePermissionTable
import database.table.PermittedAssessmentMethodForModuleTable
import database.view.ModuleViewRepository
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import providers.ConfigReader
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleDeletionRepository @Inject() (
    moduleRepository: ModuleRepository,
    moduleViewRepository: ModuleViewRepository,
    configReader: ConfigReader,
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  /**
   * Caution: This function assumes the following state
   * - the module file is deleted in the git repo
   * - the associated module branch is deleted
   * - the associated merge request is closed
   */
  def delete(module: UUID): Future[Unit] = {
    val action = for
      _ <- moduleRepository.deleteDependencies(module)
      _ <- moduleRepository.tableQuery.filter(_.id === module).delete
      moduleDraftQuery = TableQuery[ModuleDraftTable].filter(_.module === module)
      _ <- TableQuery[ModuleReviewTable].filter(_.moduleDraft.in(moduleDraftQuery.map(_.module))).delete
      _ <- moduleDraftQuery.delete
      _ <- TableQuery[ModuleUpdatePermissionTable].filter(_.module === module).delete
      _ <- TableQuery[PermittedAssessmentMethodForModuleTable].filter(_.module === module).delete
      _ <- TableQuery[ModuleCompanionTable].filter(_.module === module).delete
      _ <- TableQuery[CreatedModuleTable].filter(_.module === module).delete
    yield ()
    for
      _ <- db.run(action.transactionally)
      _ <- moduleViewRepository.refreshView()
    yield ()
  }
}
