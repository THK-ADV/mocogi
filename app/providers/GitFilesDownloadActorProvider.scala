/*package providers

@Singleton
final class GitFilesDownloadActorProvider @Inject() (
    system: ActorSystem,
    gitConfig: GitConfig,
    broker: GitFilesBroker,
    ws: WSClient,
    ctx: ExecutionContext
) extends Provider[GitFilesDownloadActor] {
  override def get(): GitFilesDownloadActor =
    GitFilesDownloadActor(
      system.actorOf(
        GitFilesDownloadActor.props(
          gitConfig,
          broker,
          ws,
          ctx
        )
      )
    )
}*/
