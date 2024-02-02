package database.repo

import database.table.{ModuleCompendiumListDbEntry, ModuleCompendiumListTable}
import database.view.StudyProgramViewRepository
import models.{ModuleCompendiumList, Semester}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleCompendiumListRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val studyProgramViewRepo: StudyProgramViewRepository,
    implicit val ctx: ExecutionContext
) extends Repository[
      ModuleCompendiumListDbEntry,
      ModuleCompendiumList,
      ModuleCompendiumListTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[ModuleCompendiumListTable]

  private def studyProgramQuery = studyProgramViewRepo.tableQuery

  override protected def retrieve(
      query: Query[ModuleCompendiumListTable, ModuleCompendiumListDbEntry, Seq]
  ): Future[Seq[ModuleCompendiumList]] = {
    db.run(
      query
        .join(studyProgramQuery)
        .on(_.fullPo === _.fullPo)
        .result
        .map(_.map { case (mcl, sp) =>
          ModuleCompendiumList(
            sp,
            Semester(mcl.semester),
            mcl.deUrl,
            mcl.enUrl,
            mcl.generated
          )
        }.toSeq)
    )
  }

  def allFromSemester(semester: String) =
    retrieve(tableQuery.filter(_.semester === semester))
}
