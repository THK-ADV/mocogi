package database.repo

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.PeopleImage
import database.table.PeopleImagesTable
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

@Singleton
final class PeopleImagesRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  private val tableQuery = TableQuery[PeopleImagesTable]

  def overrideWith(entries: Seq[PeopleImage]): Future[Unit] = {
    db.run(
      for {
        _ <- tableQuery.delete
        _ <- tableQuery ++= entries
      } yield ()
    )
  }
}
