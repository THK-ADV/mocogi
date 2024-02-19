package database.repo

import database.table.core.IdentityTable
import database.table.{
  ModuleTable,
  ModulePOOptionalTable,
  ModuleResponsibilityTable
}
import database.view.StudyProgramViewRepository
import models.{FullPoId, ModuleCore}
import models.core.Identity
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ElectivesRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val studyProgramViewRepository: StudyProgramViewRepository,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val tableQuery = TableQuery[ModuleTable]

  def all() =
    db.run(
      tableQuery
        .map(a => (a.id, a.title, a.abbrev))
        .join(TableQuery[ModuleResponsibilityTable].filter(_.isModuleManager))
        .on(_._1 === _.module)
        .join(TableQuery[IdentityTable])
        .on(_._2.identity === _.id)
        .join(TableQuery[ModulePOOptionalTable].map(a => (a.module, a.fullPo)))
        .on(_._1._1._1 === _._1)
        .result
        .map(_.groupBy(_._1._1._1._1).collect {
          case (_, xs) if xs.nonEmpty =>
            val module = ModuleCore(
              xs.head._1._1._1._1,
              xs.head._1._1._1._2,
              xs.head._1._1._1._3
            )
            val moduleManagement =
              xs.map(a => Identity.fromDbEntry(a._1._2, Nil)).distinctBy(_.id)
            val fullPodIds = xs.map(a => FullPoId(a._2._2))
            (module, moduleManagement, fullPodIds)
        })
    )
}
