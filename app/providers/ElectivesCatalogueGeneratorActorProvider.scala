package providers

import akka.actor.ActorSystem
import catalog.ElectivesCatalogueGeneratorActor
import database.repo.ElectivesRepository
import database.view.StudyProgramViewRepository
import git.api.GitAvailabilityChecker

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ElectivesCatalogueGeneratorActorProvider @Inject() (
    system: ActorSystem,
    gitAvailabilityChecker: GitAvailabilityChecker,
    electivesRepository: ElectivesRepository,
    studyProgramViewRepo: StudyProgramViewRepository,
    ctx: ExecutionContext,
    configReader: ConfigReader
) extends Provider[ElectivesCatalogueGeneratorActor] {
  override def get() = new ElectivesCatalogueGeneratorActor(
    system.actorOf(
      ElectivesCatalogueGeneratorActor.props(
        gitAvailabilityChecker,
        electivesRepository,
        studyProgramViewRepo,
        ctx,
        configReader.tmpFolderPath,
        configReader.electivesCatalogueFolderPath
      )
    )
  )
}
