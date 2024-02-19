package providers

import catalog.ElectivesCatalogService
import database.repo.ElectivesRepository
import database.view.StudyProgramViewRepository

import javax.inject.{Inject, Provider}
import scala.concurrent.ExecutionContext

@Inject
final class ElectivesCatalogServiceProvider @Inject() (
    electivesRepository: ElectivesRepository,
    studyProgramViewRepo: StudyProgramViewRepository,
    configReader: ConfigReader,
    ctx: ExecutionContext
) extends Provider[ElectivesCatalogService] {
  override def get() = new ElectivesCatalogService(
    electivesRepository,
    studyProgramViewRepo,
    configReader.tmpFolderPath,
    configReader.electivesCatalogOutputFolderPath,
    ctx
  )
}
