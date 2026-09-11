package database.repo.core

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import database.table.core.*
import models.core.Identity
import models.core.Identity.Person
import models.PeopleImage
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import service.image.PeopleImageUpdateActor.Remove
import service.image.PeopleImageUpdateActor.Update
import slick.jdbc.JdbcProfile

@Singleton
class IdentityRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    @Named("PeopleImageUpdateActor") imageUpdater: Provider[ActorRef],
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with CrudRepository[Identity] {
  import profile.api.*

  protected val tableQuery      = TableQuery[IdentityTable]
  private val peopleImagesQuery = TableQuery[PeopleImagesTable]

  def list(): Future[Seq[Identity]] =
    db.run(tableQuery.result).map(_.map(Identity.fromDbEntry))

  // Inserts into identity table and updates the person's image
  def create(input: Identity): Future[Identity] =
    for {
      _ <- db.run(tableQuery += Identity.toDbEntry(input))
      _ <- updateImage(input)
    } yield input

  // Updates identity. On success: refresh the person's image
  def update(id: String, input: Identity): Future[Int] = {
    val identity = tableQuery.filter(_.id === id)
    val action   = identity.forUpdate.result.headOption.flatMap {
      case Some(current) if current.kind != input.kind => DBIO.failed(new Identity.KindChangeNotAllowed)
      case Some(_)                                     => identity.update(Identity.toDbEntry(input))
      case None                                        => DBIO.successful(0)
    }
    for {
      updated <- db.run(action.transactionally)
      _       <- if updated > 0 then updateImage(input) else Future.unit
    } yield updated
  }

  private def updateImage(identity: Identity): Future[Unit] =
    identity match {
      case p: Person =>
        val message   = p.websiteUrl.fold(Remove(p.id))(Update(p.id, _))
        given Timeout = Timeout(10.seconds)
        (imageUpdater.get() ? message).map(_ => ())
      case _ => Future.unit
    }

  def getCampusIds(ids: List[String]): Future[Seq[CampusId]] =
    db.run(
      tableQuery
        .filter(a => a.id.inSet(ids) && a.isPerson && a.campusId.isDefined)
        .map(_.campusId.get)
        .result
        .map(_.map(CampusId.apply))
    )

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
    db.run(tableQuery.joinLeft(peopleImagesQuery).on(_.id === _.person).result)

  def replaceImages(entries: Seq[PeopleImage]): Future[Option[Int]] =
    db.run(
      for {
        _           <- peopleImagesQuery.delete
        updateCount <- peopleImagesQuery ++= entries
      } yield updateCount
    )

  def replaceImage(
      personId: String,
      expectedWebsiteUrl: Option[String],
      entry: Option[PeopleImage]
  ): Future[Int] =
    db.run(
      tableQuery
        .filter(_.id === personId)
        .map(_.websiteUrl)
        .forUpdate
        .result
        .headOption
        .flatMap {
          case Some(currentWebsiteUrl) if currentWebsiteUrl == expectedWebsiteUrl =>
            entry.fold(peopleImagesQuery.filter(_.person === personId).delete)(peopleImagesQuery.insertOrUpdate)
          case _ => DBIO.successful(0)
        }
        .transactionally
    )
}
