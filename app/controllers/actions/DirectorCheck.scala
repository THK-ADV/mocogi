package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.PersonAction.PersonRequest
import database.repo.core.StudyProgramPersonRepository
import models.UniversityRole
import play.api.mvc.ActionFilter
import play.api.mvc.Result

trait DirectorCheck { self: PermissionCheck =>
  protected def studyProgramPersonRepository: StudyProgramPersonRepository

  def hasRoleInStudyProgram(role: List[UniversityRole], studyProgram: String) =
    new ActionFilter[PersonRequest] {
      protected override def filter[A](
          request: PersonRequest[A]
      ): Future[Option[Result]] =
        toResult(
          studyProgramPersonRepository
            .hasRoles(request.person.id, studyProgram, role),
          request.request
        )

      protected override def executionContext: ExecutionContext = ctx
    }
}
