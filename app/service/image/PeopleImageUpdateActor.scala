package service.image

import javax.inject.Inject

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.blocking
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import database.repo.core.IdentityRepository
import models.PeopleImage
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.Status
import org.apache.pekko.Done
import play.api.Logging
import service.image.PeopleImageUpdateActor.Remove
import service.image.PeopleImageUpdateActor.Update
import service.image.PeopleImageUpdateActor.UpdateAll

object PeopleImageUpdateActor {
  case object UpdateAll
  case class Update(personId: String, websiteUrl: String)
  case class Remove(personId: String)
}

final class PeopleImageUpdateActor @Inject() (
    repo: IdentityRepository,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case UpdateAll =>
      logger.info("start updating images…")
      val browser = JsoupBrowser()
      val request = for {
        people <- repo.allPeople()
        images = people.par
          .flatMap(person => person.websiteUrl.flatMap(extractImage(person.id, _, browser)))
          .toList
        count <- repo.replaceImages(images)
      } yield count.getOrElse(0)

      request.onComplete {
        case Success(count) =>
          logger.info(s"successfully updated $count entries")
        case Failure(e) =>
          logger.error("failed to update images", e)
      }
    case Update(personId, websiteUrl) =>
      val replyTo = sender()
      Future(blocking(extractImage(personId, websiteUrl, JsoupBrowser())))
        .flatMap(repo.replaceImage(personId, Some(websiteUrl), _))
        .onComplete {
          case Success(_) =>
            logger.info(s"successfully updated image for $personId")
            replyTo ! Done
          case Failure(e) =>
            logger.error(s"failed to update image for $personId", e)
            replyTo ! Status.Failure(e)
        }
    case Remove(personId) =>
      val replyTo = sender()
      repo.replaceImage(personId, None, None).onComplete {
        case Success(_) =>
          logger.info(s"successfully removed image for $personId")
          replyTo ! Done
        case Failure(e) =>
          logger.error(s"failed to remove image for $personId", e)
          replyTo ! Status.Failure(e)
      }
  }

  private def extractImage(personId: String, websiteUrl: String, browser: Browser): Option[PeopleImage] = {
    val profileImageUrl = browser.get(websiteUrl) >?> element(".profile-bg img.pse-img-bg") >?> attr("src")
    profileImageUrl.flatten.map(url => PeopleImage(personId, "https://th-koeln.de" + url))
  }
}
