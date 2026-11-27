package database.repo

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.CreatedModuleTable
import database.table.ModuleCompanionTable
import database.table.ModuleDraftTable
import database.table.ModulePermittedAssessmentMethodTable
import database.table.ModuleRelationTable
import database.table.ModuleReviewTable
import database.table.ModuleUpdatePermissionTable
import database.view.ModuleViewRepository
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleDeletionRepository @Inject() (
    moduleRepository: ModuleRepository,
    moduleViewRepository: ModuleViewRepository,
    examListRepository: ExamListRepository,
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
    val moduleDraftQuery = TableQuery[ModuleDraftTable].filter(_.module === module)
    val action           = TableQuery[ModuleRelationTable].filter(_.parent === module).map(_.child).result.flatMap { children =>
      moduleRepository.reconcilingExams(children)(
        DBIO.seq(
          moduleRepository.deleteRelations(module),
          moduleRepository.deleteDependencies(module),
          moduleRepository.tableQuery.filter(_.id === module).delete,
          TableQuery[ModuleReviewTable].filter(_.moduleDraft.in(moduleDraftQuery.map(_.module))).delete,
          moduleDraftQuery.delete,
          TableQuery[ModuleUpdatePermissionTable].filter(_.module === module).delete,
          TableQuery[ModulePermittedAssessmentMethodTable].filter(_.module === module).delete,
          TableQuery[ModuleCompanionTable].filter(_.module === module).delete,
          TableQuery[CreatedModuleTable].filter(_.module === module).delete
        )
      )
    }
    for
      _ <- db.run(action.transactionally)
      _ <- moduleViewRepository.refreshView()
      _ <- examListRepository.removeUnrelatedModules(List(module))
    yield ()
  }
}
