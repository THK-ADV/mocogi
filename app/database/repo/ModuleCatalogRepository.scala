package database.repo

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.ModuleCatalogDbEntry
import database.table.ModuleCatalogTable
import database.view.StudyProgramViewRepository
import models.artifact.PublishedDocument
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

  def all(): Future[Seq[PublishedDocument]] = {
    val studyProgramView = studyProgramViewRepository.tableQuery.filter(_.specializationId.isEmpty)
    val query            = tableQuery
      .join(studyProgramView)
      .on(_.po === _.poId)
      .result
      .map(_.map {
        case (catalog, studyProgram) =>
          PublishedDocument(studyProgram, Semester(catalog.semester), catalog.date, catalog.url)
      })
    db.run(query)
  }

  def createOrUpdate(po: String, semester: String, date: LocalDate, url: String): Future[Int] =
    db.run(tableQuery.insertOrUpdate(ModuleCatalogDbEntry(po, semester, date, url)))
}
