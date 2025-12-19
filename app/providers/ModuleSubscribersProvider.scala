package providers

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import git.subscriber.*
import org.apache.pekko.actor.ActorRef

@Singleton
final class ModuleSubscribersProvider @Inject() (
    @Named("ModuleDatabaseActor") moduleDatabaseActor: ActorRef,
) extends Provider[ModuleSubscribers] {
  override def get(): ModuleSubscribers = ModuleSubscribers(List(moduleDatabaseActor))
}
