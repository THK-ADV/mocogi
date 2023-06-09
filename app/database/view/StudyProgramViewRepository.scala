package database.view

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class StudyProgramAtomic(
    po: String,
    studyProgram: String,
    grade: String,
    version: Int,
    specialization: Option[String]
)

@Singleton
final class StudyProgramViewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with MaterializedView {
  import profile.api._

  override def name: String = "study_program_atomic"

  final class StudyProgramView(tag: Tag)
      extends Table[StudyProgramAtomic](tag, name) {

    def po = column[String]("po_abbrev")

    def studyProgram = column[String]("sp_label")

    def grade = column[String]("grade_label")

    def version = column[Int]("po_version")

    def specialization = column[Option[String]]("spec_label")

    override def * = (
      po,
      studyProgram,
      grade,
      version,
      specialization
    ) <> (StudyProgramAtomic.tupled, StudyProgramAtomic.unapply)
  }

  val tableQuery = TableQuery[StudyProgramView]

  def all(): Future[Seq[StudyProgramAtomic]] =
    db.run(tableQuery.result)
}
