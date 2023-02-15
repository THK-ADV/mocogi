/*package git.publisher

object ModuleCompendiumPublisher {
  def props(
      parsingValidator: ModuleCompendiumParsingValidator,
      subscribers: ModuleCompendiumSubscribers,
      ctx: ExecutionContext
  ) = Props(
    new ModuleCompendiumPublisherImpl(parsingValidator, subscribers, ctx)
  )

  private final class ModuleCompendiumPublisherImpl(
      private val parsingValidator: ModuleCompendiumParsingValidator,
      private val subscribers: ModuleCompendiumSubscribers,
      private implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {
    override def receive = { case NotifySubscribers(changes) =>
      parse(
        changes,
        (p, r) => subscribers.added(changes.commitId, changes.timestamp, p, r),
        (p, r) =>
          subscribers.modified(changes.commitId, changes.timestamp, p, r),
        p => subscribers.removed(changes.commitId, changes.timestamp, p)
      )
    }

    private def parse(
        changes: Changes,
        added: (GitFilePath, Try[ModuleCompendium]) => Unit,
        modified: (GitFilePath, Try[ModuleCompendium]) => Unit,
        removed: GitFilePath => Unit
    ): Unit = {
      def go(
          xs: List[(GitFilePath, GitFileContent)],
          onComplete: (GitFilePath, Try[ModuleCompendium]) => Unit
      ): Unit = {
        xs foreach { case (path, content) =>
          parsingValidator
            .parse(content.value, path)
            .onComplete(res => onComplete(path, res.map(_._1)))
        }
      }
      go(changes.added, added)
      go(changes.modified, modified)
      changes.removed.foreach(removed)
    }
  }

  private case class NotifySubscribers(changes: Changes)
}

@Singleton
case class ModuleCompendiumPublisher(private val value: ActorRef) {
  def notifySubscribers(changes: Changes): Unit =
    value ! NotifySubscribers(changes)
}*/
