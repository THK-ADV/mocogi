package providers

import akka.actor.ActorSystem
import catalog.PreviewMergeActor
import database.repo.ModuleCatalogGenerationRequestRepository
import git.api.GitMergeRequestApiService

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

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
