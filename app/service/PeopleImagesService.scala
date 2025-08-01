package service

import javax.inject.Inject

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.PeopleImagesRepository
import database.table.PeopleImage
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import service.core.IdentityService

final class PeopleImagesService @Inject() (
    private val repo: PeopleImagesRepository,
    private val identityService: IdentityService,
    private implicit val ctx: ExecutionContext
) {
  def updateAll(): Future[Unit] = {
    val browser = JsoupBrowser()
    for {
      people <- identityService.allPeople()
      entries = people.par
        .collect {
          case p if p.websiteUrl.isDefined =>
            val profileImageUrl =
              browser.get(p.websiteUrl.get) >?> element(".profile-bg img.pse-img-bg") >?> attr("src")
            profileImageUrl.flatten.map(url => (p.id, url))
        }
        .collect { case Some((id, url)) => PeopleImage(id, "https://th-koeln.de" + url) }
        .toList
      _ <- repo.overrideWith(entries)
    } yield ()
  }
}
