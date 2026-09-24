package database.repo

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.ModuleCatalogTable
import database.view.StudyProgramViewRepository
import models.ModuleCatalog
import models.Semester
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleCatalogRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    studyProgramViewRepository: StudyProgramViewRepository,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  private val tableQuery = TableQuery[ModuleCatalogTable]

  def all(): Future[Seq[ModuleCatalog]] = {
    val studyProgramView = studyProgramViewRepository.tableQuery.filter(_.specializationId.isEmpty)
    val query            = tableQuery
      .join(studyProgramView)
      .on(_.po === _.poId)
      .result
      .map(_.map {
        case (catalog, studyProgram) =>
          ModuleCatalog(studyProgram, Semester(catalog.semester), catalog.date, catalog.url)
      })
    db.run(query)
  }
}
