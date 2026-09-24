package database.repo

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.ExamListDbEntry
import database.table.ExamListTable
import database.view.StudyProgramViewRepository
import models.artifact.PublishedDocument
import models.Semester
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ExamListRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val studyProgramViewRepository: StudyProgramViewRepository,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  private val tableQuery = TableQuery[ExamListTable]

  def all(): Future[Seq[PublishedDocument]] = {
    val studyProgramView = studyProgramViewRepository.tableQuery.filter(_.specializationId.isEmpty)
    val query            = tableQuery
      .join(studyProgramView)
      .on(_.po === _.poId)
      .result
      .map(_.map {
        case (examList, studyProgram) =>
          PublishedDocument(studyProgram, Semester(examList.semester), examList.date, examList.url)
      })
    db.run(query)
  }

  def createOrUpdate(po: String, semester: String, date: LocalDate, url: String) =
    db.run(tableQuery.insertOrUpdate(ExamListDbEntry(po, semester, date, url)))
}
