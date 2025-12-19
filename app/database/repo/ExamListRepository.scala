package database.repo

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.ExamListDbEntry
import database.table.ExamListTable
import database.view.StudyProgramViewRepository
import models.ExamList
import models.Semester
import models.StudyProgramView
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

  /**
   * TODO: this implementation is very inefficient, because the filtering happens in scala.
   * TODO: A native psql function should be better
   */
  def eachLatest(): Future[Seq[ExamList]] = {
    val studyProgramView = studyProgramViewRepository.tableQuery.filter(_.specializationId.isEmpty)
    val now              = LocalDate.now
    val current          = Semester.current(now).id
    val query = tableQuery
      .join(studyProgramView)
      .on(_.po === _.poId)
      .result
      .map(
        _.groupBy(_._1.po)
          .map {
            case (po, xs) =>
              val (examList, studyProgram) = xs.maxBy(e => Semester(e._1.semester))
              ExamList(studyProgram, Semester(examList.semester), examList.date, examList.url)
          }
          .toSeq
      )
    db.run(query)
  }

  def createOrUpdate(po: String, semester: String, date: LocalDate, url: String) =
    db.run(tableQuery.insertOrUpdate(ExamListDbEntry(po, semester, date, url)))
}
