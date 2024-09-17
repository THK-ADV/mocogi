package controllers.actions

import controllers.actions.PersonAction.PersonRequest
import database.repo.core.StudyProgramPersonRepository
import models.UniversityRole
import play.api.mvc.{ActionFilter, Result}

import scala.concurrent.{ExecutionContext, Future}

trait DirectorCheck { self: PermissionCheck =>
  protected def studyProgramPersonRepository: StudyProgramPersonRepository

  def hasRoleInStudyProgram(role: List[UniversityRole], studyProgram: String) =
    new ActionFilter[PersonRequest] {
      override protected def filter[A](
          request: PersonRequest[A]
      ): Future[Option[Result]] =
        toResult(
          studyProgramPersonRepository
            .hasRoles(request.person.id, studyProgram, role),
          request.request
        )

      override protected def executionContext: ExecutionContext = ctx
    }
}
