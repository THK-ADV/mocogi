package database.repo

import basedata.StudyProgramPreview
import database.table.StudyProgramTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class StudyProgramRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[
      StudyProgramPreview,
      StudyProgramPreview,
      StudyProgramTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  protected val tableQuery = TableQuery[StudyProgramTable]

  override protected def retrieve(
      query: Query[StudyProgramTable, StudyProgramPreview, Seq]
  ) = db.run(query.result)
}
