/*package providers

@Singleton
final class ModuleCompendiumPublisherProvider @Inject() (
    system: ActorSystem,
    parsingValidator: ModuleCompendiumParsingValidator,
    subscribers: ModuleCompendiumSubscribers,
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumPublisher] {
  override def get() = ModuleCompendiumPublisher(
    system.actorOf(
      ModuleCompendiumPublisher.props(parsingValidator, subscribers, ctx)
    )
  )
}*/
