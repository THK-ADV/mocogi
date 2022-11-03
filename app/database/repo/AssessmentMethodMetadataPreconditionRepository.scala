package database.repo

import database.table.{
  MetadataAssessmentMethodPreconditionDbEntry,
  MetadataAssessmentMethodPreconditionTable
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
      MetadataAssessmentMethodPreconditionDbEntry,
      MetadataAssessmentMethodPreconditionDbEntry,
      MetadataAssessmentMethodPreconditionTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery =
    TableQuery[MetadataAssessmentMethodPreconditionTable]

  override protected def retrieve(
      query: Query[
        MetadataAssessmentMethodPreconditionTable,
        MetadataAssessmentMethodPreconditionDbEntry,
        Seq
      ]
  ) =
    db.run(query.result)
}
