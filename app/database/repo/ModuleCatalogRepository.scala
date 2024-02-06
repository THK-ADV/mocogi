package database.repo

import database.table.{ModuleCatalogEntry, ModuleCatalog}
import database.view.StudyProgramViewRepository
import models.{ModuleCatalogList, Semester}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
  import profile.api._

  protected val tableQuery = TableQuery[ModuleCatalog]

  private def studyProgramQuery = studyProgramViewRepo.tableQuery

  override protected def retrieve(
      query: Query[ModuleCatalog, ModuleCatalogEntry, Seq]
  ): Future[Seq[ModuleCatalogList]] = {
    db.run(
      query
        .join(studyProgramQuery)
        .on(_.fullPo === _.fullPo)
        .result
        .map(_.map { case (mcl, sp) =>
          ModuleCatalogList(
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
