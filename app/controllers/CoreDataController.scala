package controllers

import java.sql.SQLException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import controllers.actions.UserResolveAction
import database.repo.core.*
import database.repo.schedule.RoomRepository
import database.repo.JSONRepository
import database.repo.PermissionRepository
import database.view.ModuleViewRepository
import database.view.StudyProgramViewRepository
import models.core.*
import permission.AdminCheck
import play.api.libs.json.*
import play.api.mvc.AbstractController
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.EssentialAction
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import security.ClientErrorResponse

/**
 * Read, create and update core data domain models without caching. Reads are public, writes require admin permissions.
 * Entries with a user defined id (all except teaching units and rooms) are checked for uniqueness on creation.
 */
@Singleton
final class CoreDataController @Inject() (
    cc: ControllerComponents,
    jsonRepository: JSONRepository,
    cache: ResourceCache,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    locations: LocationRepository,
    languages: LanguageRepository,
    statuses: StatusRepository,
    assessmentMethods: AssessmentMethodRepository,
    moduleTypes: ModuleTypeRepository,
    seasons: SeasonRepository,
    identities: IdentityRepository,
    pos: PORepository,
    degrees: DegreeRepository,
    studyPrograms: StudyProgramRepository,
    specializations: SpecializationRepository,
    teachingUnitRepository: TeachingUnitRepository,
    roomRepository: RoomRepository,
    studyProgramViewRepository: StudyProgramViewRepository,
    moduleViewRepository: ModuleViewRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AdminCheck
    with UserResolveAction {

  private final class Resource[A: Reads: Writes](repo: CrudRepository[A], generatedId: Boolean = false) {
    def all(): Future[Result] =
      repo.list().map(xs => Ok(Json.toJson(xs)))

    private val idReads: Reads[String] =
      if generatedId then Reads.of[UUID].map(_.toString)
      else Reads.of[String].filter(JsonValidationError("ID darf nicht leer sein"))(!_.isBlank)

    def create(json: JsObject): Future[Result] = {
      val input =
        if generatedId && !json.keys.contains("id") then json + ("id" -> JsString(UUID.randomUUID().toString))
        else json
      validate(input) { a =>
        repo.create(a).map(a => Created(Json.toJson(a))).recover {
          case e: SQLException if e.getSQLState == "23505" =>
            Conflict(Json.obj("message" -> "ID existiert bereits"))
        }
      }
    }

    def update(id: String, json: JsObject): Future[Result] =
      withId(id) { canonicalId =>
        val bodyId = json.value.get("id").fold[JsResult[String]](JsSuccess(canonicalId))(_.validate(idReads))
        bodyId match {
          case JsSuccess(value, _) if value == canonicalId =>
            validate(json + ("id" -> JsString(canonicalId)))(a =>
              repo.update(canonicalId, a).map(n => if n == 0 then NotFound else NoContent)
            )
          case _ => Future.successful(BadRequest(Json.obj("message" -> "ID muss mit der URL übereinstimmen")))
        }
      }

    private def withId(id: String)(f: String => Future[Result]): Future[Result] =
      idReads.reads(JsString(id)).fold(invalid, f)

    private def validate(json: JsObject)(f: A => Future[Result]): Future[Result] =
      (json \ "id")
        .validate(idReads)
        .flatMap(id => (json + ("id" -> JsString(id))).validate[A])
        .fold(invalid, f)

    private def invalid(
        errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]
    ): Future[Result] =
      Future.successful(BadRequest(Json.obj("message" -> "Ungültige Eingabe", "errors" -> JsError.toJson(errors))))
  }

  private val resources: Map[String, Resource[?]] = Map(
    "locations"         -> Resource(locations),
    "languages"         -> Resource(languages),
    "status"            -> Resource(statuses),
    "assessmentmethods" -> Resource(assessmentMethods),
    "moduletypes"       -> Resource(moduleTypes),
    "seasons"           -> Resource(seasons),
    "identities"        -> Resource(identities),
    "pos"               -> Resource(pos),
    "degrees"           -> Resource(degrees),
    "studyprograms"     -> Resource(studyPrograms),
    "specializations"   -> Resource(specializations),
    "teachingunits"     -> Resource(teachingUnitRepository, generatedId = true),
    "rooms"             -> Resource(roomRepository, generatedId = true)
  )

  // Handle data constraints before the authorization action handles failed requests.
  private def withResource(entity: String)(f: Resource[?] => Future[Result]) =
    resources
      .get(entity.toLowerCase)
      .fold(Future.successful(NotFound(Json.obj("message" -> s"unknown entity $entity"))))(f)
      .recover {
        case e: Identity.KindChangeNotAllowed =>
          BadRequest(Json.obj("message" -> e.getMessage))
        case e: SQLException if e.getSQLState == "23503" =>
          Conflict(Json.obj("message" -> "Der Eintrag verweist auf fehlende Daten oder wird noch verwendet"))
        case e: SQLException if Option(e.getSQLState).exists(s => s.startsWith("22") || s.startsWith("23")) =>
          BadRequest(Json.obj("message" -> "Die Eingabe verletzt eine Datenbankbedingung"))
      }

  private def admin = auth.andThen(resolveUser).andThen(isAdmin)

  private def refreshViews() =
    for {
      _ <- studyProgramViewRepository.refreshView()
      _ <- moduleViewRepository.refreshView()
    } yield ()

  def all(entity: String): Action[AnyContent] =
    Action.async(_ => withResource(entity)(_.all()))

  private def write(entity: String, request: RequestHeader)(f: Resource[?] => Future[Result]): Future[Result] =
    withResource(entity)(f)
      .flatMap { result =>
        if result.header.status == CREATED || result.header.status == NO_CONTENT then
          refreshViews().andThen { case _ => cache.invalidate(entity) }.map(_ => result)
        else Future.successful(result)
      }
      .recover { case NonFatal(e) => clientErrors.internalServerError(request, e) }

  def cachedAll(entity: String): EssentialAction =
    cache(entity, 1.hour)(all(entity))

  def create(entity: String): Action[JsObject] =
    admin.async(parse.json[JsObject])(r => write(entity, r)(_.create(r.body)))

  def update(entity: String, id: String): Action[JsObject] =
    admin.async(parse.json[JsObject])(r => write(entity, r)(_.update(id, r.body)))

  def rooms(): EssentialAction =
    cache("rooms", 1.hour) {
      Action.async(_ => jsonRepository.allRooms().map(Ok(_)))
    }

  def teachingUnits(): EssentialAction =
    cache("teachingunits", 1.hour) {
      Action.async(_ => jsonRepository.allTeachingUnits().map(Ok(_)))
    }
}
