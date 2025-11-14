package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.StudyProgramPersonRepository
import models.UniversityRole
import play.api.mvc.ActionFilter
import play.api.mvc.Result

trait DirectorCheck {
  protected def studyProgramPersonRepository: StudyProgramPersonRepository
  protected implicit def ctx: ExecutionContext

  def hasRoleInStudyProgram(role: List[UniversityRole], studyProgram: String) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = ???
//        continueAsAdmin(
//          request.request,
//          otherwise = studyProgramPersonRepository.hasRoles(request.person.id, studyProgram, role)
//        )

      protected override def executionContext: ExecutionContext = ctx
    }
}
