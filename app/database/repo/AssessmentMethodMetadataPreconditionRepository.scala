package database.repo

import database.table.{
  AssessmentMethodMetadataPreconditionDbEntry,
  AssessmentMethodMetadataPreconditionTable
}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AssessmentMethodMetadataPreconditionRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[
      AssessmentMethodMetadataPreconditionDbEntry,
      AssessmentMethodMetadataPreconditionDbEntry,
      AssessmentMethodMetadataPreconditionTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery =
    TableQuery[AssessmentMethodMetadataPreconditionTable]

  override protected def retrieve(
      query: Query[
        AssessmentMethodMetadataPreconditionTable,
        AssessmentMethodMetadataPreconditionDbEntry,
        Seq
      ]
  ) =
    db.run(query.result)
}
