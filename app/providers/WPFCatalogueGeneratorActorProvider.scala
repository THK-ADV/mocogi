package providers

import akka.actor.ActorSystem
import catalog.WPFCatalogueGeneratorActor
import database.repo.WPFRepository
import database.view.StudyProgramViewRepository
import git.api.GitAvailabilityChecker

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class WPFCatalogueGeneratorActorProvider @Inject() (
    system: ActorSystem,
    gitAvailabilityChecker: GitAvailabilityChecker,
    wpfRepository: WPFRepository,
    studyProgramViewRepo: StudyProgramViewRepository,
    ctx: ExecutionContext,
    configReader: ConfigReader
) extends Provider[WPFCatalogueGeneratorActor] {
  override def get() = new WPFCatalogueGeneratorActor(
    system.actorOf(
      WPFCatalogueGeneratorActor.props(
        gitAvailabilityChecker,
        wpfRepository,
        studyProgramViewRepo,
        ctx,
        configReader.tmpFolderPath,
        configReader.wpfCatalogueFolderPath
      )
    )
  )
}
