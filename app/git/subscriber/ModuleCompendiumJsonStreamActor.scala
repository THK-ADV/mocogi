package git.subscriber

import akka.actor.{Actor, Props}
import git.publisher.ModuleCompendiumPublisher.OnUpdate
import parsing.types.ModuleCompendium
import play.api.Logging
import play.api.libs.json.JsValue

object ModuleCompendiumJsonStreamActor {
  def props(toJson: ModuleCompendium => JsValue) = Props(
    new ModuleCompendiumJsonStreamActor(toJson)
  )
}

private final class ModuleCompendiumJsonStreamActor(
    toJson: ModuleCompendium => JsValue
) extends Actor
    with Logging {

  override def receive = { case OnUpdate(changes, _) =>
    changes.added.foreach { case (_, mc) =>
      val json = toJson(mc)
      logger.info(
        s"new module compendium with id ${mc.metadata.id}. payload: $json"
      )
    }
    changes.modified.foreach { case (_, mc) =>
      val json = toJson(mc)
      logger.info(
        s"modified module compendium with id ${mc.metadata.id}: payload: $json"
      )
    }
    changes.removed.foreach { path =>
      logger.info(
        s"need to delete module compendium with path ${path.value}"
      )
    }
  }
}
