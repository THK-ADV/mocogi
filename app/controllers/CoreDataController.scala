package controllers

import database.repo.JSONRepository
import play.api.cache.Cached
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

// TODO: move everything to core data
@Singleton
final class CoreDataController @Inject()(
  cc: ControllerComponents,
  val cached: Cached,
  jsonRepository: JSONRepository,
  implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def rooms(): Action[AnyContent] =
    Action.async(_ => jsonRepository.allRooms().map(Ok(_)))

  def teachingUnits(): Action[AnyContent] =
    Action.async(_ => jsonRepository.allTeachingUnits().map(Ok(_)))
}
