package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import database.repo.Repository
import database.table.core.*
import models.core.Identity
import models.core.Identity.Person
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

  def getByCampusId(campusId: CampusId): Future[Option[Identity.Person]] =
    db.run(
      tableQuery
        .filter(a => a.campusId === campusId.value && a.isPerson)
        .result
        .map(p => Option.when(p.size == 1)(Identity.toPersonUnsafe(p.head)))
    )

  def allIds() =
    db.run(tableQuery.map(_.id).result)

  def deleteMany(ids: Seq[String]) =
    db.run(tableQuery.filter(_.id.inSet(ids)).delete)

  private given GetResult[String] =
    GetResult(_.nextString())

  def getUserInfo(id: String, campusId: String): Future[String] = {
    val query = sql"select get_user_info($id::text, $campusId::text)".as[String].head
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
}
