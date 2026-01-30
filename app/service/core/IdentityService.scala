package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.IdentityRepository
import models.core.Identity
import models.core.Identity.toDbEntry
import parsing.core.IdentityFileParser
import play.api.libs.json.*

@Singleton
final class IdentityService @Inject() (
    val repo: IdentityRepository,
    implicit val ctx: ExecutionContext
) extends YamlService[Identity] {

  override def parser =
    Future.successful(IdentityFileParser.parser())

  override def createOrUpdateMany(xs: Seq[Identity]): Future[Seq[Identity]] =
    repo.createOrUpdateMany(xs.map(toDbEntry)).map(_ => xs)

  override def all(): Future[Seq[Identity]] =
    repo.all().map(_.map(Identity.fromDbEntry))

  def allWithImages(): Future[JsValue] =
    repo
      .allWithImages()
      .map(entries =>
        JsArray(entries.map {
          case (db, img) =>
            val id   = Identity.fromDbEntry(db)
            val json = Json.toJson(id).as[JsObject]
            json + (("imageUrl", img.fold(JsNull)(i => JsString(i.imageUrl))))
        })
      )
}
