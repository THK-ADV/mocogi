package database.repo

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import catalog.ModuleCatalogList
import database.table.ModuleCatalog
import database.table.ModuleCatalogEntry
import database.view.StudyProgramViewRepository
import models.Semester
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleCatalogRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val studyProgramViewRepo: StudyProgramViewRepository,
    implicit val ctx: ExecutionContext
) extends Repository[
      ModuleCatalogEntry,
      ModuleCatalogList,
      ModuleCatalog
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  protected val tableQuery = TableQuery[ModuleCatalog]

  private def studyProgramQuery = studyProgramViewRepo.tableQuery

  protected override def retrieve(
      query: Query[ModuleCatalog, ModuleCatalogEntry, Seq]
  ): Future[Seq[ModuleCatalogList]] = {
    db.run(
      query
        .join(studyProgramQuery)
        .on(_.fullPo === _.fullPo)
        .result
        .map(_.map {
          case (mcl, sp) =>
            catalog.ModuleCatalogList(
              sp,
              Semester(mcl.semester),
              mcl.url,
              mcl.generated
            )
        }.toSeq)
    )
  }

  def allFromSemester(semester: String) =
    retrieve(tableQuery.filter(_.semester === semester))
}
