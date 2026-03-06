package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.JSONRepository
import play.api.cache.Cached
import play.api.mvc.AbstractController
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents

// TODO: move everything to core data
@Singleton
final class CoreDataController @Inject() (
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
