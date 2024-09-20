package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import catalog.PreviewMergeActor
import database.repo.ModuleCatalogGenerationRequestRepository
import git.api.GitMergeRequestApiService
import org.apache.pekko.actor.ActorSystem

@Singleton
final class PreviewMergeActorProvider @Inject() (
    system: ActorSystem,
    mergeRequestApi: GitMergeRequestApiService,
    moduleCatalogGenerationRequestRepo: ModuleCatalogGenerationRequestRepository,
    configReader: ConfigReader,
    ctx: ExecutionContext
) extends Provider[PreviewMergeActor] {
  override def get() =
    new PreviewMergeActor(
      system.actorOf(
        PreviewMergeActor.props(
          mergeRequestApi,
          moduleCatalogGenerationRequestRepo,
          configReader.bigBangLabel,
          ctx
        )
      )
    )
}
