package service.image

import javax.inject.Inject

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import database.repo.core.IdentityRepository
import database.table.PeopleImage
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import org.apache.pekko.actor.Actor
import play.api.Logging
import service.image.PeopleImageUpdateActor.Update

object PeopleImageUpdateActor {
  case object Update
}

final class PeopleImageUpdateActor @Inject() (
    repo: IdentityRepository,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case Update =>
      logger.info("start updating images…")
      val browser = JsoupBrowser()
      val request = for {
        people <- repo.allPeople()
        images = people.par
          .collect {
            case p if p.websiteUrl.isDefined =>
              val profileImageUrl =
                browser.get(p.websiteUrl.get) >?> element(".profile-bg img.pse-img-bg") >?> attr("src")
              profileImageUrl.flatten.map(url => (p.id, url))
          }
          .collect { case Some((id, url)) => PeopleImage(id, "https://th-koeln.de" + url) }
          .toList
        count <- repo.replaceImages(images)
      } yield count.getOrElse(0)

      request.onComplete {
        case Success(count) =>
          logger.info(s"successfully updated $count entries")
        case Failure(e) =>
          logger.error("failed to update images", e)
      }
  }
}
