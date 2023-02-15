package database.repo

import database.table.StudyFormTypeTable
import models.core.StudyFormType
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class StudyFormTypeRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[StudyFormType, StudyFormType, StudyFormTypeTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  protected val tableQuery = TableQuery[StudyFormTypeTable]

  override protected def retrieve(
      query: Query[StudyFormTypeTable, StudyFormType, Seq]
  ) =
    db.run(query.result)
}
