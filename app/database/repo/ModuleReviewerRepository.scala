package database.repo

import database.table.{ModuleReviewerTable, POTable}
import models.{ModuleReviewer, ModuleReviewerRole}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleReviewerRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[ModuleReviewer, ModuleReviewer, ModuleReviewerTable]
    with HasDatabaseConfigProvider[JdbcProfile] {

  import database.table.moduleReviewerRoleColumnType
  import profile.api._

  protected val tableQuery = TableQuery[ModuleReviewerTable]

  override protected def retrieve(
      query: Query[ModuleReviewerTable, ModuleReviewer, Seq]
  ): Future[Seq[ModuleReviewer]] =
    db.run(query.result)

  def delete(id: UUID) =
    db.run(
      tableQuery
        .filter(_.id === id)
        .delete
    )

  def getAll(roles: Seq[ModuleReviewerRole], pos: Set[String]) =
//    tableQuery
//      .filter(_.role === role)
//      .join(TableQuery[POTable].filter(_.abbrev.inSet(pos)))
//      .on(_.studyProgram === _.studyProgram)
//      .map(_._1)
    db.run(
      tableQuery
        .filter(a =>
          a.role.inSet(roles) && a.studyProgram.in(
            TableQuery[POTable]
              .filter(_.abbrev.inSet(pos))
              .map(_.studyProgram)
          )
        )
        .result
    )
}
