package providers

import akka.actor.ActorSystem
import compendium.WPFCatalogueGeneratorActor
import database.repo.{PORepository, WPFRepository}
import git.api.GitAvailabilityChecker

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class WPFCatalogueGeneratorActorProvider @Inject() (
    system: ActorSystem,
    gitAvailabilityChecker: GitAvailabilityChecker,
    wpfRepository: WPFRepository,
    poRepository: PORepository,
    ctx: ExecutionContext,
    configReader: ConfigReader
) extends Provider[WPFCatalogueGeneratorActor] {
  override def get() = new WPFCatalogueGeneratorActor(
    system.actorOf(
      WPFCatalogueGeneratorActor.props(
        gitAvailabilityChecker,
        wpfRepository,
        poRepository,
        ctx,
        configReader.tmpFolderPath,
        configReader.wpfCatalogueFolderPath
      )
    )
  )
}
