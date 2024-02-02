package database.repo

import database.table.{
  IdentityTable,
  ModuleCompendiumTable,
  POOptionalTable,
  ResponsibilityTable
}
import database.view.StudyProgramViewRepository
import models.{FullPoId, Module}
import models.core.Identity
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class WPFRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val studyProgramViewRepository: StudyProgramViewRepository,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val tableQuery = TableQuery[ModuleCompendiumTable]

  def all() =
    db.run(
      tableQuery
        .map(a => (a.id, a.title, a.abbrev))
        .join(TableQuery[ResponsibilityTable].filter(_.isModuleManager))
        .on(_._1 === _.metadata)
        .join(TableQuery[IdentityTable])
        .on(_._2.identity === _.id)
        .join(TableQuery[POOptionalTable].map(a => (a.metadata, a.fullPo)))
        .on(_._1._1._1 === _._1)
        .result
        .map(_.groupBy(_._1._1._1._1).collect {
          case (_, xs) if xs.nonEmpty =>
            val module = Module(
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
