package providers

import catalog.ElectivesCatalogService
import database.repo.ElectivesRepository
import database.view.StudyProgramViewRepository
import git.api.GitAvailabilityChecker

import javax.inject.{Inject, Provider}
import scala.concurrent.ExecutionContext

@Inject
final class ElectivesCatalogServiceProvider @Inject() (
    gitAvailabilityChecker: GitAvailabilityChecker,
    electivesRepository: ElectivesRepository,
    studyProgramViewRepo: StudyProgramViewRepository,
    configReader: ConfigReader,
    ctx: ExecutionContext
) extends Provider[ElectivesCatalogService] {
  override def get() = new ElectivesCatalogService(
    gitAvailabilityChecker,
    electivesRepository,
    studyProgramViewRepo,
    configReader.tmpFolderPath,
    configReader.electivesCatalogueFolderPath,
    configReader.mcPath,
    ctx
  )
}
