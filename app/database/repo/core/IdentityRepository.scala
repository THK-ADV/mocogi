package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import database.repo.Repository
import database.table.core.*
import database.table.PeopleImagesTable
import models.core.Identity
import models.core.Identity.Person
import models.PeopleImage
import models.UserInfo
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.GetResult
import slick.jdbc.JdbcProfile

@Singleton
class IdentityRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[IdentityDbEntry, IdentityDbEntry, IdentityTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  protected val tableQuery = TableQuery[IdentityTable]

  protected override def retrieve(query: Query[IdentityTable, IdentityDbEntry, Seq]) =
    db.run(query.result)

  def getCampusIds(ids: List[String]): Future[Seq[CampusId]] =
    db.run(
      tableQuery
        .filter(a => a.id.inSet(ids) && a.isPerson && a.campusId.isDefined)
        .map(_.campusId.get)
        .result
        .map(_.map(CampusId.apply))
    )

  def allIds() =
    db.run(tableQuery.map(_.id).result)

  def deleteMany(ids: Seq[String]) =
    db.run(tableQuery.filter(_.id.inSet(ids)).delete)

  private given GetResult[UserInfo] =
    GetResult(r => UserInfo(r.nextBoolean(), r.nextBoolean(), r.nextBoolean(), r.nextInt(), r.nextInt(), None, false))

  def getUserInfo(id: String, campusId: String): Future[UserInfo] = {
    val query = sql"select * from get_user_info($id::text, $campusId::text)".as[UserInfo].head
    db.run(query)
  }

  def allByIds(ids: List[String]): Future[List[CampusId]] =
    db.run(
      tableQuery
        .filter(a => a.campusId.isDefined && a.id.inSet(ids))
        .map(_.campusId)
        .result
        .map(_.collect { case Some(id) => CampusId(id) }.toList)
    )

  def allPeople(): Future[Seq[Person]] =
    db.run(tableQuery.filter(_.isPerson).result.map(_.map(Identity.toPersonUnsafe)))

  def allWithImages(): Future[Seq[(IdentityDbEntry, Option[PeopleImage])]] =
    db.run(tableQuery.joinLeft(TableQuery[PeopleImagesTable]).on(_.id === _.person).result)

  def replaceImages(entries: Seq[PeopleImage]): Future[Option[Int]] = {
    val tq = TableQuery[PeopleImagesTable]
    db.run(
      for {
        _           <- tq.delete
        updateCount <- tq ++= entries
      } yield updateCount
    )
  }
}
